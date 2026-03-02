# 🚀 Roadmap – Payment Service (FIAP Restaurant | Tech Challenge Fase 3)

Microsserviço responsável por:
- consumir eventos de criação de pedido
- processar pagamento via serviço externo (eventualmente disponível)
- publicar eventos de pagamento aprovado ou pendente
- reprocessar automaticamente pagamentos pendentes quando o serviço externo voltar

Requisitos-chave atendidos:
- Comunicação assíncrona via Kafka (eventos obrigatórios)
- Resiliência com Resilience4j (Circuit Breaker, Retry, Timeout + Fallback)
- Persistência em PostgreSQL (pagamentos e pendências)
- Arquitetura em camadas (Clean/Hexagonal)

---

## ✅ Definições iniciais (contratos)

### Tópicos Kafka (mínimo)
- `pedido.criado` (entrada) :contentReference[oaicite:1]{index=1}
- `pagamento.aprovado` (saída) :contentReference[oaicite:2]{index=2}
- `pagamento.pendente` (saída) :contentReference[oaicite:3]{index=3}

### Status essenciais
**PaymentStatus**
- `APROVADO`
- `PENDENTE`

(Se quiser enriquecer: `PROCESSANDO`, `RECUSADO`, `ERRO`, mas o desafio não exige.)

### Integração externa
- Serviço “processamento-pagamento-externo” fornecido como imagem, tratado como serviço externo com indisponibilidade eventual. :contentReference[oaicite:4]{index=4}

---

## FASE 1 — Base do projeto e infraestrutura local

**Objetivo:** subir o payment-service com infra completa em 1 comando.

**Entregas**
- `compose.yml` com:
    - PostgreSQL
    - Kafka
    - Kafka UI (opcional, mas ajuda a demonstrar)
    - processamento-pagamento-externo (imagem do professor)
- Flyway:
    - migração inicial para tabela de pagamentos
- Healthchecks (liveness/readiness)
- `.env.example` + `.gitignore` para `.env`

**Definition of Done**
- `docker compose up` sobe tudo
- migrations aplicadas automaticamente
- app sobe e responde `/actuator/health`

---

## FASE 2 — Modelagem de domínio + persistência

**Objetivo:** persistir pagamento e estado de pendência com integridade.

**Entregas**
- `domain/model`
    - `Payment`
    - `PaymentStatus`
    - (opcional) Value Objects: `PaymentId`, `OrderId`
- `domain/gateway`
    - `PaymentRepositoryGateway`
- `infra/persistence`
    - `PaymentEntity` + `SpringDataRepository`
    - adapter JPA implementando `PaymentRepositoryGateway`

**Sugestão de dados mínimos do Payment**
- `paymentId`
- `orderId`
- `amount`
- `status`
- `createdAt`, `updatedAt`
- (opcional) `attemptCount`, `lastError`

**Definition of Done**
- CRUD interno via repositório funciona (testes)
- salvar e atualizar status funciona

---

## FASE 3 — Integração com serviço externo (síncrona, com resiliência)

**Objetivo:** chamar o serviço externo e suportar falhas sem quebrar o fluxo.

**Entregas**
- `domain/gateway`
    - `ExternalPaymentProcessorGateway`
- `infra/http`
    - client (RestTemplate/WebClient/Feign)
- Resilience4j aplicado na chamada:
    - **Circuit Breaker**
    - **Retry**
    - **Timeout**
    - **Fallback** que:
        - marca pagamento/pedido como pendente
        - publica `pagamento.pendente` :contentReference[oaicite:5]{index=5}

**Definition of Done**
- quando externo está OK → pagamento aprovado
- quando externo falha/timeout/circuit open → pagamento fica PENDENTE e segue fluxo assíncrono

---

## FASE 4 — Kafka: consumo e publicação de eventos

**Objetivo:** implementar o fluxo mínimo orientado a eventos.

**Entregas**
- Consumer `pedido.criado`:
    - recebe `orderId`, `amount`, `clientId` (se existir), etc.
    - cria `Payment` e tenta processar externo
- Producer:
    - publica `pagamento.aprovado` quando processado
    - publica `pagamento.pendente` no fallback :contentReference[oaicite:6]{index=6}

**Definition of Done**
- evento `pedido.criado` dispara tentativa de pagamento
- eventos de saída aparecem no Kafka UI (ótimo para o vídeo)

---

## FASE 5 — Worker de reprocessamento automático (pendências)

**Objetivo:** quando o payment-service voltar a funcionar, reprocessar automaticamente pendências. :contentReference[oaicite:7]{index=7}

**Estratégia (uma escolha)**
1) `pagamento.pendente` vai para um tópico.
2) Worker consome o tópico e tenta reprocessar.
3) Se aprovado:
    - atualiza status no banco
    - publica `pagamento.aprovado` (ou um evento de atualização)

**Cuidados técnicos**
- idempotência por `orderId` (não aprovar duas vezes)
- controle de tentativas (`attemptCount`) e backoff
- DLQ opcional para “pendência eterna” (não obrigatório)

**Definition of Done**
- desligo o serviço externo → pendências acumulam
- ligo o serviço externo → worker reprocessa e aprova

---

## FASE 6 — Testes e evidências para avaliação

**Objetivo:** garantir robustez e facilitar a validação do professor.

**Entregas**
- Unit tests (domínio + usecases)
- Integração (preferencial):
    - Testcontainers Postgres
    - Kafka (Testcontainers ou Embedded Kafka)
    - mock do externo (WireMock) OU subir a imagem externa no compose de teste
- Cenários obrigatórios:
    - externo OK → `pagamento.aprovado`
    - externo OFF → `pagamento.pendente`
    - externo volta → reprocessa → `pagamento.aprovado`

**Definition of Done**
- pipeline CI executa testes
- cenários acima demonstráveis

---

## 📚 FASE 7 — Documentação (para vídeo e entrega)

**Objetivo:** facilitar a avaliação.

**Entregas**
- Diagrama de sequência do fluxo:
    - pedido.criado → payment-service → externo → pagamento.aprovado/pagamento.pendente → worker → aprovado
- Lista explícita de “pontos de resiliência” (circuit breaker / retry / timeout / fallback) :contentReference[oaicite:8]{index=8}
- Postman/Bruno:
    - (mesmo que o payment-service seja dirigido por eventos, ter endpoints internos de consulta ajuda):
        - `GET /payments/{id}`
        - `GET /payments?orderId=...`

---

## ✅ Checklist final (payment-service)

- [ ] Sobe com docker compose (db + kafka + externo)
- [ ] Consome `pedido.criado`
- [ ] Chama serviço externo com Resilience4j
- [ ] Falha vira pendência e publica `pagamento.pendente`
- [ ] Worker reprocessa automaticamente
- [ ] Aprovação publica `pagamento.aprovado`
- [ ] Persistência consistente (idempotência por orderId)
- [ ] Testes cobrindo sucesso/falha/reprocessamento
- [ ] Documentação e diagramas prontos para o vídeo
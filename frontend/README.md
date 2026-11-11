# TurnoFácil — Frontend

React + Vite (JavaScript). Consume la API de `turnofacil-api`.

## Comandos

```bash
npm install
npm run dev            # http://localhost:5173 (proxy /api -> http://localhost:8080)
npm run lint
npm test
npm run test:coverage  # genera coverage/lcov.info (lo usa SonarCloud)
npm run build
```

Para probar contra la API local: en la raíz del repo, `./mvnw spring-boot:run` (perfil dev con H2).

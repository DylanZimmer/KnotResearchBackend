# SageMath computation service

This service owns symbolic computation only. The Spring Boot application builds
the Alexander matrix and sends it to `POST /alexander`; the React application
does not call this service directly.

## API

- `GET /health`
- `POST /alexander`

An Alexander request contains a square `matrix`. Each nonzero entry is an
object `{ "x": ..., "y": ... }` representing `x + y*t`; a JSON `null` is zero.
`removeRow` and `removeColumn` are optional zero-based indexes and default to
the last row and column.

## Run locally

On Windows, start SageMath and Spring Boot together with:

```powershell
.\dev.ps1
```

On macOS or Linux:

```shell
sh ./dev.sh
```

The launcher builds and starts the SageMath container, waits for its health
endpoint, and runs Spring Boot in the foreground. Pressing Ctrl+C stops both.

Alternatively, run both applications as containers from the repository root:

```shell
docker compose up --build
```

Spring Boot is then available on port 8080 and SageMath on port 8000. Compose
sets `SAGE_MATH_URL=http://sage-math:8000` for the backend. When Spring Boot is
run outside Compose, its default is `http://localhost:8000`.

Run the SageMath endpoint tests inside its image:

```shell
docker compose build sage-math
docker compose run --rm --entrypoint sage sage-math -python -m pytest
```

Run Java integration tests with `./mvnw test` (or `mvnw.cmd test` on Windows).

## Deployment

Deploy the Spring Boot and SageMath containers on the same private container
network. Set `SAGE_MATH_URL` on Spring Boot to the SageMath service's internal
URL. Do not publicly route the SageMath port unless operational tooling needs
it. Configure database credentials on the Spring Boot container as before.
The connect and calculation timeouts can be overridden with
`SAGE_MATH_CONNECT_TIMEOUT` and `SAGE_MATH_READ_TIMEOUT`.

Additional computations should get their own Pydantic request/response models
and route in `app/main.py`, plus matching Java DTOs and a method on
`SageMathService`. This keeps knot construction and orchestration in Java while
sharing the containerized Sage runtime for future Jones, Conway, Seifert, and
signature calculations.

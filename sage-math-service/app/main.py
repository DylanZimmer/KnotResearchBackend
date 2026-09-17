from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, model_validator
from sage.all import ZZ, matrix


class LinearPolynomial(BaseModel):
    x: int
    y: int


class AlexanderRequest(BaseModel):
    matrix: list[list[LinearPolynomial | None]]
    removeRow: int | None = None
    removeColumn: int | None = None

    @model_validator(mode="after")
    def validate_square_matrix(self):
        size = len(self.matrix)
        if size == 0 or any(len(row) != size for row in self.matrix):
            raise ValueError("matrix must be non-empty and square")
        for index in (self.removeRow, self.removeColumn):
            if index is not None and not 0 <= index < size:
                raise ValueError("removed row and column must be valid zero-based indexes")
        return self


class AlexanderResponse(BaseModel):
    polynomial: str
    determinant: int


app = FastAPI(title="Knots SageMath Service", version="1.0.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/alexander", response_model=AlexanderResponse)
def alexander(request: AlexanderRequest) -> AlexanderResponse:
    try:
        ring = ZZ["t"]
        t = ring.gen()
        symbolic_rows = [
            [ring.zero() if entry is None else entry.x + entry.y * t for entry in row]
            for row in request.matrix
        ]
        alexander_matrix = matrix(ring, symbolic_rows)
        size = alexander_matrix.nrows()
        remove_row = size - 1 if request.removeRow is None else request.removeRow
        remove_column = size - 1 if request.removeColumn is None else request.removeColumn
        kept_rows = [index for index in range(size) if index != remove_row]
        kept_columns = [index for index in range(size) if index != remove_column]
        minor = alexander_matrix.matrix_from_rows_and_columns(kept_rows, kept_columns)
        polynomial = minor.det()
        return AlexanderResponse(
            polynomial=str(polynomial), determinant=int(abs(polynomial(-1)))
        )
    except Exception as exception:
        raise HTTPException(status_code=422, detail=f"Alexander calculation failed: {exception}") from exception

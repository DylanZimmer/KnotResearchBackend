from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health():
    assert client.get("/health").json() == {"status": "ok"}


def test_figure_eight_alexander_polynomial():
    response = client.post(
        "/alexander",
        json={
            "matrix": [
                [{"x": -1, "y": 1}, {"x": 1, "y": 0}, {"x": 0, "y": -1}],
                [{"x": 1, "y": 0}, {"x": -2, "y": 1}, {"x": 1, "y": -1}],
                [None, None, None],
            ]
        },
    )
    assert response.status_code == 200
    assert response.json() == {"polynomial": "t^2 - 3*t + 1", "determinant": 5}


def test_unknot_empty_minor_has_unit_polynomial():
    response = client.post("/alexander", json={"matrix": [[None]]})
    assert response.status_code == 200
    assert response.json() == {"polynomial": "1", "determinant": 1}


def test_rejects_non_square_matrix():
    response = client.post("/alexander", json={"matrix": [[None, None]]})
    assert response.status_code == 422

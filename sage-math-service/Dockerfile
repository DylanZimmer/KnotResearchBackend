FROM sagemath/sagemath:10.6

WORKDIR /app

COPY requirements.txt ./
RUN sage -pip install --no-cache-dir -r requirements.txt

COPY app/ app/
COPY tests/ tests/

EXPOSE 8000

ENTRYPOINT ["sage", "-python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

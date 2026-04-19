FROM python:3.12-slim

WORKDIR /app

# pdfplumber (default) needs no extra system libs on slim.
# PyMuPDF (fallback/classifier) needs build-essential for its C extensions.
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY ./app /app/app

# Port for parser service
EXPOSE 8090

# Standard uvicorn launch
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090", "--workers", "2"]

# Backend Deployment & Implementation Guide

This document provides step-by-step instructions on how to set up, run, and deploy the Python ZKP Backend.

## 1. Local Development Setup

### Prerequisites
- Python 3.9 or higher
- pip (Python Package Manager)
- Virtualenv (recommended)

### Step-by-Step Installation

1.  **Navigate to the backend directory:**
    ```bash
    cd backend
    ```

2.  **Create a virtual environment:**
    ```bash
    python -m venv venv
    ```

3.  **Activate the virtual environment:**
    - **Windows (PowerShell):**
        ```powershell
        .\venv\Scripts\Activate
        ```
    - **Linux/macOS:**
        ```bash
        source venv/bin/activate
        ```

4.  **Install dependencies:**
    ```bash
    pip install -r requirements.txt
    ```

5.  **Run the server (Development Mode):**
    ```bash
    uvicorn main:app --reload --port 8001
    ```
    - The server will be available at `http://localhost:8001`.
    - Swagger UI documentation: `http://localhost:8001/docs`.

---

## 2. Production Deployment (Recommended)

For production, we recommend using **Docker** to containerize the application and **Nginx** as a reverse proxy.

### Option A: Docker Deployment

1.  **Create a `Dockerfile`** in the `backend/` directory:
    ```dockerfile
    FROM python:3.9-slim

    WORKDIR /app

    COPY requirements.txt .
    RUN pip install --no-cache-dir -r requirements.txt

    COPY . .

    CMD ["uvicorn", "main:app", "--port", "8001"]
    ```

2.  **Build the Docker image:**
    ```bash
    docker build -t ekyc-backend .
    ```

3.  **Run the container:**
    ```bash
    docker run -d -p 8001:8001 --name ekyc-server ekyc-backend
    ```

### Option B: Manual Linux Deployment (Ubuntu/CentOS)

1.  **Install System Dependencies:**
    ```bash
    sudo apt update
    sudo apt install python3-pip python3-venv nginx
    ```

2.  **Clone & Setup:**
    Follow the "Local Development Setup" steps to install dependencies in `/var/www/ekyc-backend`.

3.  **Setup Systemd Service (Keep alive):**
    Create `/etc/systemd/system/ekyc.service`:
    ```ini
    [Unit]
    Description=Gunicorn instance to serve eKYC API
    After=network.target

    [Service]
    User=ubuntu
    Group=www-data
    WorkingDirectory=/var/www/ekyc-backend
    Environment="PATH=/var/www/ekyc-backend/venv/bin"
    ExecStart=/var/www/ekyc-backend/venv/bin/uvicorn main:app --workers 4 --port 8001

    [Install]
    WantedBy=multi-user.target
    ```
    Start service: `sudo systemctl start ekyc && sudo systemctl enable ekyc`

4.  **Configure Nginx (Reverse Proxy & SSL):**
    Create `/etc/nginx/sites-available/ekyc`:
    ```nginx
    server {
        listen 80;
        server_name api.yourdomain.com;

        location / {
            proxy_pass http://127.0.0.1:8001;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
    ```
    Enable site: `sudo ln -s /etc/nginx/sites-available/ekyc /etc/nginx/sites-enabled`
    Restart Nginx: `sudo systemctl restart nginx`

---

## 3. Database Management

- **Default**: The app uses SQLite (`ekyc.db`) by default.
- **Resetting DB**: To clear all data, simply delete the `ekyc.db` file and restart the server.
- **Production DB**: To use PostgreSQL:
    1. Install driver: `pip install psycopg2-binary`
    2. Update `database.py`:
       ```python
       SQLALCHEMY_DATABASE_URL = "postgresql://user:password@localhost/dbname"
       ```

## 4. Troubleshooting

- **"ID already enrolled"**: The server remembers the ID hash. Delete `ekyc.db` to reset.
- **"Challenge mismatch"**: Ensure the Client and Server clocks are synchronized.
- **Connection Refused (Android)**: Use `10.0.2.2` to access localhost from the Android Emulator.

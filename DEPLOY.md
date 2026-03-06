# Deploying Lumina Crawl

## Option 1: Render (Recommended — Free tier)

Best for a quick public demo. No credit card needed for the free web service.

### Steps

1. **Push to GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR_USER/website-quality-analyzer.git
   git push -u origin main
   ```

2. **Create Render account** at [render.com](https://render.com)

3. **Deploy via Blueprint**
   - Dashboard → **New** → **Blueprint**
   - Connect your GitHub repo
   - Render reads `render.yaml` automatically
   - Add `GEMINI_API_KEY` when prompted (mark as secret)

4. **Wait ~5 min** for Docker build. Your app will be live at:
   `https://lumina-crawl.onrender.com` (or similar)

### Render environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Optional | Gemini AI recommendations |
| `GEMINI_MODEL` | No | Default: `gemini-3.1-flash-lite` |
| `CRAWL_MAX_PAGES` | No | Default: 30 |
| `PORT` | Auto | Set by Render |

> **Note:** The `render` profile uses in-memory H2 — scan history resets on restart. Fine for demos.

---

## Option 2: Railway

1. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub**
2. Select your repo
3. Railway auto-detects the `Dockerfile`
4. Set environment variables:
   ```
   SPRING_PROFILES_ACTIVE=render
   GEMINI_API_KEY=your_key
   GEMINI_MODEL=gemini-3.1-flash-lite
   ```
5. Deploy — Railway assigns a public URL

---

## Option 3: Docker on any VPS (DigitalOcean, AWS, etc.)

```bash
# On your server
git clone https://github.com/YOUR_USER/website-quality-analyzer.git
cd website-quality-analyzer
cp .env.example .env
# Edit .env with your keys

docker compose up --build -d
```

App runs on port **8080**. Put Nginx/Caddy in front for HTTPS.

### Nginx reverse proxy snippet

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_read_timeout 180s;
        proxy_connect_timeout 180s;
    }
}
```

---

## Option 4: Fly.io

```bash
# Install flyctl: https://fly.io/docs/hands-on/install-flyctl/
fly launch --dockerfile Dockerfile
fly secrets set GEMINI_API_KEY=your_key SPRING_PROFILES_ACTIVE=render
fly deploy
```

---

## Production with MySQL

For persistent scan history, use the `prod` profile with MySQL:

```bash
SPRING_PROFILES_ACTIVE=prod
MYSQL_HOST=your-db-host
MYSQL_PORT=3306
MYSQL_DATABASE=quality_analyzer
MYSQL_USER=wqa
MYSQL_PASSWORD=your_password
GEMINI_API_KEY=your_key
```

Use **PlanetScale**, **Railway MySQL**, or **AWS RDS** for managed MySQL.

---

## Health check

```
GET /api/health
→ { "status": "UP", "service": "Website Quality Analyzer" }
```

Use this URL for uptime monitoring and platform health checks.

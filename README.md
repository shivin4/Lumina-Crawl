# Lumina Crawl — AI-Powered Website Quality Analyzer

Automatically crawl any website and generate a comprehensive quality report with broken links, duplicate content detection, SEO analysis, URL scoring, crawl depth visualization, and AI-powered recommendations via Gemini.

![Tech Stack](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green) ![Jsoup](https://img.shields.io/badge/Jsoup-Crawler-blue)

## Features

| Module | Description |
|--------|-------------|
| **Web Crawler** | BFS & DFS graph traversal with configurable depth and page limits |
| **URL Analyzer** | Length, special chars, dynamic params, readability — SEO score per URL |
| **Regex URL Filter** | Classify URLs as Useful / Irrelevant / Duplicate |
| **HTML SEO Evaluator** | Title, meta description, H1/H2, image alt text detection |
| **Content Quality** | Word count, keyword density, Flesch readability, thin content detection |
| **Site Structure** | Interactive crawl hierarchy tree visualization |
| **AI Engine** | Gemini API recommendations with local fallback |
| **Persistence** | MySQL (production) / H2 (development) report history |

## Quick Start (Development)

### Prerequisites
- Java 17+
- Maven 3.8+

### Run locally (H2 in-memory DB)

```bash
mvn spring-boot:run
```

Open **http://localhost:8080**

### With Gemini AI

```bash
export GEMINI_API_KEY=your_key_here
mvn spring-boot:run
```

## Deploy Online (Render / Railway / Fly.io)

See **[DEPLOY.md](DEPLOY.md)** for step-by-step hosting guides.

**Quickest path:** Push to GitHub → [Render Blueprint](https://render.com) → connect repo → add `GEMINI_API_KEY` → live in ~5 min.

## Production Deployment (Docker + MySQL)

```bash
cp .env.example .env
# Edit .env with your GEMINI_API_KEY and MySQL credentials

docker compose up --build -d
```

App: **http://localhost:8080**  
MySQL: `localhost:3306`

### Single JAR deployment

```bash
mvn clean package -DskipTests
java -jar target/website-quality-analyzer-1.0.0.jar \
  --spring.profiles.active=prod \
  --MYSQL_HOST=your-db-host \
  --GEMINI_API_KEY=your_key
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/analyze` | Run full site analysis |
| `GET` | `/api/reports` | List recent reports |
| `GET` | `/api/reports/{id}` | Get saved report by ID |
| `GET` | `/api/health` | Health check |

### Example Request

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com","maxPages":30,"maxDepth":4,"crawlStrategy":"BFS"}'
```

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Frontend   │────▶│  Spring Boot API │────▶│   MySQL     │
│  (Static)   │     │                  │     │   / H2      │
└─────────────┘     │  ┌────────────┐  │     └─────────────┘
                    │  │ WebCrawler │  │     ┌─────────────┐
                    │  │ BFS / DFS  │  │────▶│  Jsoup      │
                    │  └────────────┘  │     └─────────────┘
                    │  ┌────────────┐  │     ┌─────────────┐
                    │  │ Analyzers  │  │────▶│  Gemini AI  │
                    │  └────────────┘  │     └─────────────┘
                    └──────────────────┘
```

## Regex URL Filter Rules

- `.*login.*` — Login pages
- `.*cart.*` — Shopping cart
- `.*checkout.*` — Checkout
- `.*\?sessionid=.*` — Session parameters
- `.*wp-admin.*` — Admin panels
- `.*\.pdf$` — PDF files

## Environment Variables

See [`.env.example`](.env.example) for all configuration options.

## License

MIT

# ContentPulse — Spring Boot Backend

AI-powered social media analytics platform backend.

## Architecture
```
POST /api/posts
      ↓
  PostService → Kafka Producer (post-events)
                      ↓
              Kafka Consumer (3 partitions)
                   ↙        ↘
        Groq AI (Llama 3.3)  TrendingService (Redis)
              ↓
        PostService.updateAiAnalysis()
              ↓
        WebSocket broadcast → /topic/posts → React Frontend
```

## Modules
| Package | Description |
|---|---|
| `auth`           | JWT login/register, Spring Security |
| `post`           | Post CRUD, Kafka Producer |
| `analytics`      | Groq AI Sentiment, stats endpoints |
| `trending`       | Tag frequency, Redis-cached rankings |
| `recommendation` | Collaborative Filtering |
| `kafka`          | Producer + Consumer wiring |
| `config`         | Security, WebSocket, Redis, Kafka topics |
| `common`         | Global exception handler |

## Quick Start (Docker)

```bash
# 1. Set your Groq API key (free at console.groq.com)
export GROQ_API_KEY=your_key_here

# 2. Start all services
docker-compose up -d

# 3. Backend runs at http://localhost:8080
```

## Quick Start (Local)

```bash
# Start infra only
docker-compose up -d postgres redis zookeeper kafka

# Run Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-DGROQ_API_KEY=your_key"
```

## API Endpoints

### Auth
| Method | Endpoint | Body | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | `{username, email, password}` | Public |
| POST | `/api/auth/login`    | `{username, password}` | Public |
| GET  | `/api/auth/me`       | — | Bearer token |

### Posts
| Method | Endpoint | Params | Auth |
|---|---|---|---|
| GET    | `/api/posts`         | `page, size, sentiment` | Bearer |
| POST   | `/api/posts`         | `{content}` | Bearer |
| POST   | `/api/posts/{id}/like` | — | Bearer |
| DELETE | `/api/posts/{id}`    | — | Bearer |

### Analytics
| Method | Endpoint | Params |
|---|---|---|
| GET | `/api/analytics/summary`    | — |
| GET | `/api/analytics/engagement` | `days=7` |
| GET | `/api/analytics/sentiment`  | `days=7` |

### Other
| GET | `/api/trending`       | — |
| GET | `/api/recommendations`| — |

## WebSocket
Connect to `ws://localhost:8080/ws` (SockJS/STOMP).  
Subscribe to `/topic/posts` to receive real-time post events after AI processing.

## Environment Variables
| Variable | Description |
|---|---|
| `GROQ_API_KEY` | Groq API key (free at console.groq.com) |
| `JWT_SECRET`   | JWT signing secret (min 32 chars) |

# MD04_SS12_Bai1: Product Display Optimization with Redis Cache

## 1. Overview
This project optimizes the product display service (`product-service`) by applying the **Cache-Aside Pattern** with **Spring Boot** and **Redis**. 

It solves the high database query load problem for frequently accessed but rarely changed product information (such as product name, description, price, and image URL).

## 2. Tech Stack & Architecture
- **Framework**: Spring Boot `4.1.1`
- **Language**: Java 21 (LTS)
- **Build Tool**: Gradle 9.7.1
- **Cache & In-Memory Store**: Redis 7.x
- **Serialization**: `RedisSerializer.json()` (Generic Jackson JSON Serialization) and `RedisSerializer.string()` for keys
- **TTL**: 30 minutes (`Duration.ofMinutes(30)`)
- **Eviction Strategy**: `@CacheEvict` triggered on product updates

## 3. Key Features & Implementation

### A. Cache-Aside Pattern (`@Cacheable`)
- The `getProductById(Long id)` method in `ProductService` is annotated with:
  ```java
  @Cacheable(value = "products", key = "#id")
  public ProductResponse getProductById(Long id)
  ```
- **Cache Miss**: On the first request, Spring Cache executes the service method, fetches from the database (simulating ~200ms latency), and stores the result in Redis with a 30-minute TTL.
- **Cache Hit**: Subsequent requests retrieve the data directly from Redis in `< 10ms` without accessing the database.

### B. Cache Invalidation (`@CacheEvict`)
- When an administrator updates a product, stale cached data must be invalidated immediately.
- The `updateProduct(Long id, UpdateProductRequest request)` method is annotated with:
  ```java
  @CacheEvict(value = "products", key = "#id")
  public ProductResponse updateProduct(Long id, UpdateProductRequest request)
  ```
- This automatically removes `products::<id>` from Redis, ensuring consistency.

### C. Redis Configuration (`RedisConfig.java`)
- Configured with `RedisCacheConfiguration`:
  - **Key Serializer**: `StringRedisSerializer`
  - **Value Serializer**: `GenericJackson2JsonRedisSerializer` / `RedisSerializer.json()` for readable JSON storage instead of JVM binary bytecode.
  - **TTL**: 30 minutes (`entryTtl(Duration.ofMinutes(30))`).
  - **Null values**: Disabled (`disableCachingNullValues()`).

---

## 4. REST API Reference

| Method | Endpoint | Description | Cache Behavior |
|--------|----------|-------------|----------------|
| `GET`  | `/api/products/{id}` | Get product details by ID | `@Cacheable(value = "products", key = "#id")` |
| `PUT`  | `/api/products/{id}` | Update product details | `@CacheEvict(value = "products", key = "#id")` |
| `POST` | `/api/products` | Create a new product | Direct save to database |
| `GET`  | `/api/products` | List all products | Direct read from database |

---

## 5. Verification & Benchmark Evidence

### Benchmark Results
1. **First Request (Cache Miss - Database Read):**
   ```bash
   curl -s -w "\nTime: %{time_total}s\n" http://localhost:8081/api/products/3
   ```
   - **Response Time**: `0.221s` (~221ms)
   - **Log**: `--> [DATABASE ACCESS] Querying database for Product ID: 3`

2. **Subsequent Requests (Cache Hit - Redis Read):**
   ```bash
   curl -s -w "\nTime: %{time_total}s\n" http://localhost:8081/api/products/3
   ```
   - **Response Time**: `0.005s - 0.006s` (< 10ms)
   - **Log**: No database query executed.

3. **TTL & Redis Inspection:**
   ```bash
   docker exec medicine-redis redis-cli TTL "products::3"
   # Output: ~1800 (seconds)

   docker exec medicine-redis redis-cli GET "products::3"
   # Output: {"@class":"com.example.productservice.dto.ProductResponse","id":3,"name":"AirPods Pro 2",...}
   ```

4. **Cache Eviction on Update:**
   ```bash
   curl -X PUT http://localhost:8081/api/products/3 -H "Content-Type: application/json" -d '{"name":"AirPods Pro 2 (USB-C)","price":6190000}'
   docker exec medicine-redis redis-cli EXISTS "products::3"
   # Output: 0 (Key evicted from cache)
   ```

---

## 6. How to Run

### Prerequisites
- Java 21+
- Redis running on `localhost:6379` (e.g. `docker run -d -p 6379:6379 redis:7-alpine`)

### Run Tests
```powershell
.\gradlew.bat test
```

### Run Application
```powershell
.\gradlew.bat bootRun
```
Access the service at `http://localhost:8081/api/products/1`.

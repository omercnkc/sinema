# Ön yüz imajı (rules §5.9): Node 14 ile üretim derlemesi, nginx (root olmayan) ile statik sunum.
# Çalıştırma: docker compose up --build frontend

# --- 1. aşama: derleme (K50: Node 14.18.1) ---
FROM node:14.18.1-alpine3.14 AS derleme
WORKDIR /uygulama
# Önce yalnızca bağımlılık dosyaları: kaynak değişince npm ci katmanı önbellekten gelir
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Tarayıcının gideceği gateway adresi derlemeye gömülür (compose GATEWAY_HOST_PORT'tan türetir)
ARG API_TABAN_ADRESI=http://localhost:8080/api/v1
RUN echo "$API_TABAN_ADRESI" | grep -Eq '^https?://[A-Za-z0-9.:/_-]+$' \
      || { echo "Geçersiz API_TABAN_ADRESI: $API_TABAN_ADRESI" >&2; exit 1; } \
 && sed -i "s#apiTabanAdresi: '[^']*'#apiTabanAdresi: '${API_TABAN_ADRESI}'#" src/environments/environment.prod.ts \
 && grep -q "apiTabanAdresi: '${API_TABAN_ADRESI}'" src/environments/environment.prod.ts \
 && npx ng build --configuration production

# --- 2. aşama: sunum ---
FROM nginxinc/nginx-unprivileged:1.31.7-alpine
# /etc/nginx/templates/*.template açılışta envsubst ile conf.d'ye yazılır (API_KOKENI CSP'de kullanılır)
COPY nginx/sinema.conf.template /etc/nginx/templates/default.conf.template
COPY --from=derleme /uygulama/dist/sinema-frontend /usr/share/nginx/html
EXPOSE 8080

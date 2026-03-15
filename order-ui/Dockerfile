FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# In Docker the UI routes through nginx /api proxy — no direct host access needed
ARG VITE_ORDER_ENGINE_URL=/api
ENV VITE_ORDER_ENGINE_URL=$VITE_ORDER_ENGINE_URL

RUN npm run build

# ── Runtime ────────────────────────────────────────────────────────────────────
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

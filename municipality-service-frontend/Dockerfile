FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

EXPOSE 5173

CMD ["sh", "-lc", "VITE_API_BASE_URL=${VITE_API_BASE_URL:-http://backend:6969} npm run dev -- --host 0.0.0.0 --port 5173"]

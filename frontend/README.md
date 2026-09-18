# ExpiryMate Frontend

React + Vite frontend for ExpiryMate. It is already integrated with the API Gateway.

## Included UI/functionality

- Registration and login validation.
- Password strength, confirm password, show/hide password.
- Correct login/logout navigation without page refresh.
- Dashboard cards and Recharts visualizations.
- My Documents search/filter/edit/delete.
- Add Document with duplicate checks and PDF/JPG/PNG upload.
- File download.
- Reminders view.
- Profile name/password updates with validation.
- Lucide icons throughout the main UI.

## API integration

`src/api.js` uses:

```text
VITE_API_URL || http://localhost:8080
```

JWT is automatically attached as:

```text
Authorization: Bearer <token>
```

## Run locally

```bash
npm install
npm run dev
```

Then open the Vite URL (normally http://localhost:5173).

The backend API Gateway must be running at http://localhost:8080 unless you set another `VITE_API_URL`.

## Run with Docker

```bash
docker compose up --build
```

Open:

- Frontend: http://localhost:3000

The Docker build defaults to API Gateway `http://localhost:8080`, which is correct when the user's browser can reach the backend on the same computer.

For another backend host, create `.env`:

```text
VITE_API_URL=https://api.example.com
```

and rebuild the image.

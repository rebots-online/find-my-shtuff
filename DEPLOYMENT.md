# Deploying Your Application

This project is a static React application built with Vite. It can be deployed to various static site hosting platforms.

## General Deployment Steps

The following steps are generally applicable to platforms like Vercel, Netlify, or Firebase Hosting.

1.  **Connect Repository:**
    *   Connect your Git repository (e.g., GitHub, GitLab, Bitbucket) to your chosen hosting platform. Most platforms offer direct integration.

2.  **Build Configuration:**
    *   **Build Command:** Set this to `npm run build`. This command will compile your React application and Vite will output the static files.
    *   **Publish Directory (or Output Directory):** Set this to `dist`. This is the directory where Vite places the built static assets.

3.  **Environment Variables:**
    *   You will need to set the `GEMINI_API_KEY` in your hosting platform's environment variable settings.
    *   **Important:** Treat this API key as a secret. Do not expose it publicly. Ensure your hosting platform provides a secure way to store and use environment variables.

4.  **Deploy:**
    *   Trigger a deployment through your hosting platform's interface. This usually happens automatically after connecting your repository and configuring the build settings, especially on new pushes to your main branch.

## Platform-Specific Documentation

Please note that the specific steps or naming conventions for settings might vary slightly depending on the hosting platform you choose. Always refer to the official documentation for your selected platform for the most accurate and up-to-date instructions.

For example:
*   [Vercel Documentation](https://vercel.com/docs)
*   [Netlify Documentation](https://docs.netlify.com/)
*   [Firebase Hosting Documentation](https://firebase.google.com/docs/hosting)

## Deploying to GitHub Pages (Optional)

For simple static sites without server-side logic or protected environment variables that need to be kept from the client-side build, GitHub Pages can be an option. However, given that this application uses an API key (`GEMINI_API_KEY`), which is intended to be kept secret and used server-side or in a secure backend function, directly exposing it in a purely client-side GitHub Pages build is **not recommended**.

If you were to deploy a version without sensitive API key usage directly in the frontend bundle, the typical steps would involve:
1. Setting the `base` path in `vite.config.ts` to your repository name (e.g., `/<repository-name>/`).
2. Running `npm run build`.
3. Committing the `dist` folder (or using a GitHub Action to build and deploy to the `gh-pages` branch).

For this application, prioritize platforms that allow secure handling of environment variables for the `GEMINI_API_KEY`.

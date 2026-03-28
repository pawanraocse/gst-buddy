export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080', // Overridden at runtime by /api/config proxy
  cognito: {
    // These are fallback values only — real values are fetched from /api/config/cognito at startup.
    // Do NOT hardcode production or dev IDs here.
    userPoolId: '',
    userPoolWebClientId: '',
    region: 'us-east-1',
    domain: ''
  }
};


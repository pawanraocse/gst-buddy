export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080', // Gateway URL
  cognito: {
    userPoolId: 'ap-south-1_At0XoCqKe',
    userPoolWebClientId: '6t2vol2cju7ksv05k65egk33ut',
    region: 'ap-south-1',
    domain: 'gstbuddies-dev-2gida5dk.auth.ap-south-1.amazoncognito.com'
  },
  gtmId: '' // Left empty in dev to prevent firing analytics
};

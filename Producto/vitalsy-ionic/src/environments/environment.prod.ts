import { Capacitor } from '@capacitor/core';

export const environment = {
  production: true,
  apiUrl: Capacitor.getPlatform() === 'android'
    ? 'http://10.0.2.2:8080/api/v1'
    : '/api/v1'
};

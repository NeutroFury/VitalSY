import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.vitalsy.app',
  appName: 'VitalSY',
  webDir: 'www',
  server: {
    androidScheme: 'http'
  }
};

export default config;

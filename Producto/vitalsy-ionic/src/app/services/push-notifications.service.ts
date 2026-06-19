import { Injectable, inject } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { PushNotifications, Token, PushNotificationSchema, ActionPerformed } from '@capacitor/push-notifications';
import { ToastController } from '@ionic/angular';
import { DeviceService } from './device.service';

/**
 * Servicio de notificaciones push con @capacitor/push-notifications.
 *
 * Responsabilidades:
 * 1. Solicitar permisos al usuario (iOS/Android).
 * 2. Capturar el token FCM generado por el dispositivo.
 * 3. Enviarlo al backend vía DeviceService.
 * 4. Manejar notificaciones recibidas en primer plano (app abierta).
 * 5. Manejar taps en notificaciones (app en background/cerrada).
 */
@Injectable({
  providedIn: 'root'
})
export class PushNotificationsService {
  private deviceService = inject(DeviceService);
  private toastCtrl = inject(ToastController);

  /**
   * Inicializa el sistema de push notifications.
   * Debe llamarse en app.component.ts o al iniciar la sesión del usuario.
   * No hace nada si la plataforma es web (no soportada por Capacitor Push).
   */
  async initialize(): Promise<void> {
    // Capacitor Push solo funciona en dispositivos nativos (iOS/Android)
    if (!Capacitor.isNativePlatform()) {
      console.log('[PushNotifications] Plataforma web detectada. Push notifications deshabilitadas.');
      return;
    }

    const permResult = await PushNotifications.requestPermissions();

    if (permResult.receive === 'granted') {
      console.log('[PushNotifications] Permisos otorgados. Registrando con FCM...');
      await PushNotifications.register();
      this.registerListeners();
    } else {
      console.warn('[PushNotifications] El usuario denegó los permisos de notificación.');
    }
  }

  private registerListeners(): void {
    // ── Token FCM obtenido exitosamente ──────────────────────────────────
    PushNotifications.addListener('registration', (token: Token) => {
      console.log('[PushNotifications] Token FCM recibido:', token.value.substring(0, 20) + '...');
      const plataforma = Capacitor.getPlatform() as 'android' | 'ios';
      this.deviceService.registrarFcmToken({ fcmToken: token.value, plataforma }).subscribe({
        next: () => console.log('[PushNotifications] Token registrado en backend correctamente.'),
        error: (err) => console.error('[PushNotifications] Error registrando token en backend:', err)
      });
    });

    // ── Error de registro ────────────────────────────────────────────────
    PushNotifications.addListener('registrationError', (error: any) => {
      console.error('[PushNotifications] Error de registro FCM:', error.error);
    });

    // ── Notificación recibida CON LA APP ABIERTA (foreground) ────────────
    PushNotifications.addListener('pushNotificationReceived', async (notification: PushNotificationSchema) => {
      console.log('[PushNotifications] Notificación en foreground recibida:', notification.title);
      await this.showForegroundAlert(notification.title || '¡Alerta!', notification.body || '');
    });

    // ── El usuario tocó la notificación (desde background/cerrado) ───────
    PushNotifications.addListener('pushNotificationActionPerformed', (action: ActionPerformed) => {
      const data = action.notification.data;
      console.log('[PushNotifications] Notificación tocada. Datos:', data);
      // Aquí se puede navegar a una pantalla específica según `data.screen`
      // Por ejemplo: this.navCtrl.navigateForward('/dashboard');
    });
  }

  /**
   * Muestra un Toast de alerta urgente cuando la app está en primer plano.
   * Reemplaza la notificación del sistema operativo que no aparece en foreground.
   */
  private async showForegroundAlert(titulo: string, cuerpo: string): Promise<void> {
    const toast = await this.toastCtrl.create({
      header: titulo,
      message: cuerpo,
      duration: 6000,
      position: 'top',
      color: 'danger',
      buttons: [{ text: 'Ver', role: 'cancel' }],
      cssClass: 'vitalsy-alert-toast'
    });
    await toast.present();
  }

  /**
   * Desregistra todos los listeners y desactiva el token en el backend.
   * Llamar al hacer logout para evitar notificaciones tras cerrar sesión.
   */
  async cleanup(): Promise<void> {
    if (!Capacitor.isNativePlatform()) return;
    await PushNotifications.removeAllListeners();
  }
}

import { Injectable } from '@angular/core';
import { LocalNotifications } from '@capacitor/local-notifications';
import { NavController, AlertController, Platform } from '@ionic/angular';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(
    private navCtrl: NavController,
    private alertCtrl: AlertController,
    private platform: Platform
  ) {
    this.init();
  }

  async init() {
    if (this.platform.is('hybrid')) {
      const perm = await LocalNotifications.checkPermissions();
      if (perm.display !== 'granted') {
        await LocalNotifications.requestPermissions();
      }

      LocalNotifications.addListener('localNotificationActionPerformed', (notification) => {
        console.log('NOTIFICATION: Acción realizada:', notification);
        this.navCtrl.navigateRoot('/dashboard');
      });
    }
  }

  async scheduleUrgentNotification(title: string, body: string, type: 'critical' | 'warning' | 'info' = 'info') {
    console.log(`NOTIFICATION_LOG [${type.toUpperCase()}]: ${title} - ${body}`);

    // 1. Intentar notificación nativa (Capacitor)
    try {
      await LocalNotifications.schedule({
        notifications: [
          {
            title,
            body,
            id: Math.floor(Math.random() * 10000),
            schedule: { at: new Date(Date.now() + 1000) },
            sound: 'critical_alert.wav',
            smallIcon: 'res://ic_stat_vitalsy',
            attachments: [],
            actionTypeId: '',
            extra: null
          }
        ]
      });
    } catch (e) {
      console.warn('NOTIFICATION: No se pudo lanzar la notificación nativa (posiblemente en navegador).');
    }

    // 2. Fallback Visual para Desarrollo (Navegador/Ionic Serve)
    // Si estamos en el navegador, mostramos un Alert de Ionic para no perder la alerta
    if (!this.platform.is('hybrid')) {
      const headerColor = type === 'critical' ? '!! EMERGENCIA !!' : 'AVISO DE VITALSY';
      const alert = await this.alertCtrl.create({
        header: headerColor,
        subHeader: title,
        message: body,
        cssClass: `alert-type-${type}`, // Por si quieres darle estilos CSS neón
        buttons: [
          {
            text: 'VER DASHBOARD',
            handler: () => {
              this.navCtrl.navigateRoot('/dashboard');
            }
          }
        ]
      });
      await alert.present();
    }
  }
}

import { Injectable } from '@angular/core';
import { LocalNotifications } from '@capacitor/local-notifications';
import { RecordatorioResponse } from './recordatorio.service';
import { Capacitor } from '@capacitor/core';

@Injectable({
  providedIn: 'root'
})
export class LocalReminderService {
  
  constructor() { }

  /**
   * Pide permisos e instala las alarmas locales
   */
  async syncLocalNotifications(recordatorios: RecordatorioResponse[]) {
    if (!Capacitor.isNativePlatform()) {
      console.log('Local Notifications no soportadas en web.');
      return;
    }

    try {
      // 1. Pedir permisos
      const perms = await LocalNotifications.requestPermissions();
      if (perms.display !== 'granted') {
        console.warn('Permisos de notificaciones locales denegados.');
        return;
      }

      // 2. Cancelar todas las alarmas anteriores programadas
      const pending = await LocalNotifications.getPending();
      if (pending.notifications.length > 0) {
        await LocalNotifications.cancel(pending);
      }

      // 3. Programar las nuevas
      let notifsToSchedule: any[] = [];
      
      recordatorios.filter(r => r.activo).forEach((r) => {
        const parts = r.hora.split(':');
        const hour = parseInt(parts[0], 10);
        const minute = parseInt(parts[1], 10);
        
        const dias = r.diasRepeticion.split(',').map(d => parseInt(d.trim(), 10));

        let bodyText = 'Tienes un nuevo recordatorio en VitalSY';
        let titleText = 'Recordatorio VitalSY';
        if (r.tipo === 'MEDICION_GLUCOSA') {
          titleText = '⏰ Mide tu Glucosa';
          bodyText = 'Es hora de realizar tu medición de glucosa programada.';
        } else if (r.tipo === 'APLICACION_INSULINA') {
          titleText = '💉 Aplicación de Insulina';
          bodyText = 'Es el momento de tu dosis de insulina pautada.';
        } else if (r.tipo === 'COMIDA') {
          titleText = '🍽️ Hora de Comer';
          bodyText = 'Recuerda registrar tu ingesta de alimentos.';
        }

        dias.forEach(dia => {
          // En Capacitor schedule, domingo = 1, lunes = 2... sábado = 7
          // Si guardamos ISO: 1=Lunes, 7=Domingo
          let capacitorDay = dia === 7 ? 1 : dia + 1;

          // Generamos un ID que quepa en un entero 32-bit: ej. id_recordatorio * 10 + dia
          const notifId = (r.id * 10) + dia;

          notifsToSchedule.push({
            id: notifId,
            title: titleText,
            body: bodyText,
            schedule: {
              on: {
                weekday: capacitorDay,
                hour: hour,
                minute: minute
              },
              allowWhileIdle: true
            },
            smallIcon: 'ic_stat_name',
            actionTypeId: ''
          });
        });
      });

      if (notifsToSchedule.length > 0) {
        await LocalNotifications.schedule({ notifications: notifsToSchedule });
      }
    } catch (error) {
      console.error('Error sincronizando notificaciones locales:', error);
    }
  }
}

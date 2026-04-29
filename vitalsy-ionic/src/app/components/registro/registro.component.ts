import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { nutritionOutline, chevronBackOutline, waterOutline } from 'ionicons/icons';

import { GlucoseService } from '../../services/glucose.service';
import { inject } from '@angular/core';
import { HeaderComponent } from '../header/header.component';
import { LoadingController, AlertController } from '@ionic/angular';

@Component({
  selector: 'app-registro',
  templateUrl: './registro.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent]
})
export class RegistroComponent {
  glucose: number = 0;
  carbs: number = 0;
  date: string = new Date().toISOString().slice(0, 16);
  selectedMoment: string = 'Ayunas';
  notes: string = '';

  private glucoseService = inject(GlucoseService);
  private navCtrl = inject(NavController);
  private loadingCtrl = inject(LoadingController);
  private alertCtrl = inject(AlertController);

  moments: string[] = [
    'Ayunas', 'Pre-Desayuno', 'Post-Desayuno', 
    'Pre-Almuerzo', 'Post-Almuerzo', 'Merienda', 
    'Pre-Cena', 'Post-Cena', 'Ejercicio', 'Antes de Dormir'
  ];

  constructor() {
    addIcons({ nutritionOutline, chevronBackOutline, waterOutline });
  }

  selectMoment(moment: string) {
    this.selectedMoment = moment;
  }

  async saveRecord() {
    console.log('REGISTRO: Intentando guardar valor:', this.glucose);

    if (this.glucose <= 0) {
      const alert = await this.alertCtrl.create({
        header: 'Valor inválido',
        message: 'Por favor, introduce un valor de glucosa mayor a 0.',
        buttons: ['OK']
      });
      await alert.present();
      return;
    }

    const loader = await this.loadingCtrl.create({
      message: 'Guardando lectura...'
    });
    await loader.present();

    const tendencia = this.glucose > 180 ? 'Rising' : (this.glucose < 70 ? 'Falling' : 'Stable');
    
    this.glucoseService.saveReading(this.glucose, tendencia).subscribe({
      next: async () => {
        console.log('REGISTRO: Lectura guardada con éxito');
        await loader.dismiss();
        this.navCtrl.navigateBack('/dashboard');
      },
      error: async (err) => {
        console.error('REGISTRO: Error al guardar lectura', err);
        await loader.dismiss();
        const alert = await this.alertCtrl.create({
          header: 'Error de Conexión',
          message: 'No se pudo conectar con el servidor. Revisa tu conexión o el backend.',
          buttons: ['OK']
        });
        await alert.present();
      }
    });
  }
}

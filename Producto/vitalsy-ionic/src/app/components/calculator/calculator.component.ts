import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { 
  calculatorOutline, 
  radioOutline, 
  informationCircleOutline,
  medkitOutline
} from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';
import { LibreLinkUpService } from '../../services/librelinkup.service';
import { GlucoseService } from '../../services/glucose.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-calculator',
  templateUrl: './calculator.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent]
})
export class CalculatorComponent implements OnInit {
  private libreService = inject(LibreLinkUpService);
  private glucoseService = inject(GlucoseService);
  private userService = inject(UserService);

  currentGlucose: number | null = null;
  carbsToConsume: number | null = null;
  icRatio: number = 10;
  
  targetGlucose: number = 100;
  isSensitivy: number = 40;
  
  isCalculated: boolean = false;
  isSyncing: boolean = false;

  correctionDose: number = 0;
  carbohydrateDose: number = 0;
  totalDose: number = 0;

  constructor() {
    addIcons({ calculatorOutline, radioOutline, informationCircleOutline, medkitOutline });
  }

  ngOnInit() {
    this.loadUserProfile();
  }

  private loadUserProfile() {
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        if (profile) {
          if (profile.ratioIc) this.icRatio = profile.ratioIc;
          if (profile.factorIs) this.isSensitivy = profile.factorIs;
          
          // Establecer la glucosa objetivo como el punto medio del rango de glucosa si está disponible
          if (profile.rangoGlucosaMin && profile.rangoGlucosaMax) {
            this.targetGlucose = Math.round((profile.rangoGlucosaMin + profile.rangoGlucosaMax) / 2);
          }
        }
      },
      error: (err) => {
        console.error('No se pudo obtener el perfil del usuario para la calculadora', err);
      }
    });
  }

  handleSyncSensor() {
    if (this.isSyncing) return;
    this.isSyncing = true;

    // Fuerzo la sincronización con la nube de LibreView (LibreLinkUp)
    this.libreService.forceSync().subscribe({
      next: () => {
        this.fetchLatestReading();
      },
      error: (err) => {
        console.error('Error al sincronizar con LibreLinkUp, intentando leer último dato local', err);
        // Si falla la sincronización (ej: sin red, o credenciales erróneas), intento usar el último de la base de datos
        this.fetchLatestReading();
      }
    });
  }

  private fetchLatestReading() {
    this.glucoseService.getRecentReadings().subscribe({
      next: (readings) => {
        if (readings && readings.length > 0) {
          // Tomar la lectura más reciente de la base de datos local
          const latestReading = readings[readings.length - 1];
          this.currentGlucose = latestReading.valorMgdl;
        } else {
          console.warn('No hay lecturas guardadas.');
        }
        this.isSyncing = false;
        this.isCalculated = false;
      },
      error: (err) => {
        console.error('No se pudo obtener las lecturas de glucosa', err);
        this.isSyncing = false;
      }
    });
  }

  generateDose() {
    const g = this.currentGlucose || 0;
    const tg = this.targetGlucose || 100;
    const is = this.isSensitivy || 40;
    const c = this.carbsToConsume || 0;
    const ic = this.icRatio || 10;

    let correction = (g - tg) / is;
    if (correction < 0) correction = 0; 
    
    let carbs = c / ic;

    this.correctionDose = correction;
    this.carbohydrateDose = carbs;
    
    let total = correction + carbs;
    
    this.correctionDose = Math.round(this.correctionDose * 10) / 10;
    this.carbohydrateDose = Math.round(this.carbohydrateDose * 10) / 10;
    this.totalDose = Math.round(total * 10) / 10;

    this.isCalculated = true;
  }
}

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
import { CognitivoService, CalculoDosisRequest, CalculoDosisResponse } from '../../services/cognitivo.service';

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
  private cognitivoService = inject(CognitivoService);

  userId: number = 0;
  comidasDisponibles: string[] = [];
  selectedComida: string = '';
  customComida: string = '';

  currentGlucose: number | null = null;
  carbsToConsume: number | null = null;
  
  isCalculated: boolean = false;
  isSyncing: boolean = false;
  isCalculating: boolean = false;

  public resultado: CalculoDosisResponse | null = null;

  constructor() {
    addIcons({ calculatorOutline, radioOutline, informationCircleOutline, medkitOutline });
  }

  ngOnInit() {
    this.loadUserProfile();
  }

  private loadUserProfile() {
    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        if (profile && profile.id) {
          this.userId = profile.id;
          this.loadComidas();
        }
      },
      error: (err) => {
        console.error('No se pudo obtener el perfil del usuario para la calculadora', err);
      }
    });
  }

  private loadComidas() {
    this.cognitivoService.getComidas(this.userId).subscribe({
      next: (comidas) => {
        this.comidasDisponibles = comidas;
        if (comidas.length > 0) {
          this.selectedComida = comidas[0];
        } else {
          this.selectedComida = 'otra';
        }
      },
      error: (err) => console.error('Error al cargar comidas', err)
    });
  }

  handleSyncSensor() {
    if (this.isSyncing) return;
    this.isSyncing = true;

    this.libreService.forceSync().subscribe({
      next: () => {
        this.fetchLatestReading();
      },
      error: (err) => {
        console.error('Error al sincronizar con LibreLinkUp, intentando leer último dato local', err);
        this.fetchLatestReading();
      }
    });
  }

  private fetchLatestReading() {
    this.glucoseService.getRecentReadings().subscribe({
      next: (readings) => {
        if (readings && readings.length > 0) {
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
    if (this.currentGlucose === null || this.carbsToConsume === null) return;

    let comidaName = this.selectedComida === 'otra' ? this.customComida : this.selectedComida;
    if (!comidaName || comidaName.trim() === '') {
      comidaName = 'Desconocida';
    }

    const request: CalculoDosisRequest = {
      usuarioId: this.userId,
      nombreComida: comidaName.trim(),
      glicemiaActual: this.currentGlucose,
      carbohidratos: this.carbsToConsume
    };

    this.isCalculating = true;
    this.isCalculated = false;

    this.cognitivoService.calcularDosis(request).subscribe({
      next: (res: CalculoDosisResponse) => {
        this.resultado = res;
        this.isCalculated = true;
        this.isCalculating = false;
      },
      error: (err) => {
        console.error('Error al calcular dosis', err);
        this.isCalculating = false;
      }
    });
  }
}

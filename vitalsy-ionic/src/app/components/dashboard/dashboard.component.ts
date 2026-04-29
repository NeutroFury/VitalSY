import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { 
  pulseOutline, 
  timeOutline, 
  addOutline, 
  calculatorOutline, 
  hardwareChipOutline, 
  notificationsOutline, 
  settingsOutline,
  refreshOutline
} from 'ionicons/icons';
import { HeaderComponent } from '../header/header.component';
import { IaService, IaAnalysis } from '../../services/ia.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule, HeaderComponent]
})
export class DashboardComponent implements OnInit {
  
  iaAnalysis: IaAnalysis | null = null;
  isIaLoading = false;
  iaError: string | null = null;

  private iaService = inject(IaService);
  private navCtrl = inject(NavController);

  constructor() {
    addIcons({ 
      pulseOutline, 
      timeOutline, 
      addOutline, 
      calculatorOutline, 
      hardwareChipOutline,
      notificationsOutline,
      settingsOutline,
      refreshOutline
    });
  }

  ngOnInit() {
    this.runAiAnalysis();
  }

  runAiAnalysis() {
    this.isIaLoading = true;
    this.iaError = null;
    this.iaAnalysis = null; // Limpiamos el análisis anterior para forzar el estado de carga
    
    this.iaService.getLatestAnalysis().subscribe({
      next: (analysis) => {
        if (!analysis) {
          this.iaError = 'No hay lecturas recientes para analizar';
        } else {
          this.iaAnalysis = analysis;
        }
        this.isIaLoading = false;
      },
      error: (err) => {
        // Si el backend devuelve 204, a veces llega por aquí dependiendo de la versión de HttpClient
        if (err.status === 204) {
          this.iaError = 'No hay lecturas recientes para analizar';
        } else {
          this.iaError = 'Análisis predictivo temporalmente no disponible';
        }
        this.isIaLoading = false;
      }
    });
  }

  navigateTo(route: string) {
    this.navCtrl.navigateForward(`/${route}`);
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { 
  pulseOutline, 
  alertCircleOutline, 
  informationCircleOutline, 
  checkmarkCircleOutline,
  chevronBackOutline,
  analyticsOutline,
  warningOutline
} from 'ionicons/icons';
import { IaService, PredictiveAnalysisResponse } from '../../services/ia.service';
import { NavController } from '@ionic/angular';

@Component({
  selector: 'app-predictive',
  templateUrl: './predictive.component.html',
  styleUrls: ['./predictive.component.scss'],
  standalone: true,
  imports: [CommonModule, IonicModule]
})
export class PredictiveComponent implements OnInit {
  private iaService = inject(IaService);
  private navCtrl = inject(NavController);

  analysis: PredictiveAnalysisResponse | null = null;
  loading: boolean = true;
  error: string | null = null;

  constructor() {
    addIcons({
      pulseOutline,
      alertCircleOutline,
      informationCircleOutline,
      checkmarkCircleOutline,
      chevronBackOutline,
      analyticsOutline,
      warningOutline
    });
  }

  ngOnInit() {
    this.loadAnalysis();
  }

  loadAnalysis() {
    this.loading = true;
    this.error = null;
    
    // As per requirement, requesting a 7 days predictive analysis
    this.iaService.getPredictiveAnalysis(7).subscribe({
      next: (res) => {
        this.analysis = res;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'No se pudo cargar el análisis predictivo. Por favor, intenta de nuevo más tarde.';
        this.loading = false;
      }
    });
  }

  goBack() {
    this.navCtrl.back();
  }

  getRiskColorClass(risk: string): string {
    switch (risk?.toUpperCase()) {
      case 'BAJO': return 'risk-low';
      case 'MEDIO': return 'risk-medium';
      case 'ALTO': return 'risk-high';
      case 'CRITICO': return 'risk-critical';
      default: return 'risk-unknown';
    }
  }

  getProbabilityPercentage(prob: number): number {
    return Math.round(prob * 100);
  }
}

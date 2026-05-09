import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { IonicModule, NavController } from '@ionic/angular';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
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
import { GlucoseService } from '../../services/glucose.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule, HeaderComponent, BaseChartDirective]
})
export class DashboardComponent implements OnInit, OnDestroy {
  
  iaAnalysis: IaAnalysis | null = null;
  isIaLoading = false;
  iaError: string | null = null;
  currentReading: number = 0;
  recentHistory: any[] = [];
  private refreshSub?: Subscription;

  lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Glucosa (mg/dL)',
        borderColor: '#ccff00',
        pointBackgroundColor: '#ccff00',
        pointBorderColor: '#000',
        pointHoverBackgroundColor: '#ccff00',
        pointHoverBorderColor: '#000',
        borderWidth: 3,
        pointRadius: 0, // Ocultamos puntos por defecto para look limpio
        pointHoverRadius: 4,
        fill: true,
        tension: 0.4,
        backgroundColor: (context) => {
          const chart = context.chart;
          const chartArea = chart.chartArea;
          if (!chartArea) return 'rgba(204, 255, 0, 0.1)';
          const gradient = chart.ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          gradient.addColorStop(0, 'rgba(204, 255, 0, 0.15)');
          gradient.addColorStop(1, 'rgba(204, 255, 0, 0)');
          return gradient;
        }
      }
    ]
  };

  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#121212',
        titleColor: '#52525b',
        bodyColor: '#ccff00',
        borderColor: '#1E1E1E',
        borderWidth: 1,
        cornerRadius: 12,
        padding: 10,
        displayColors: false
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          color: '#3f3f46',
          font: { size: 9, weight: 'bold' }
        }
      },
      y: {
        grid: { display: false },
        border: { display: false },
        ticks: {
          color: '#3f3f46',
          font: { size: 9, weight: 'bold' }
        }
      }
    }
  };

  private iaService = inject(IaService);
  private glucoseService = inject(GlucoseService);
  private navCtrl = inject(NavController);
  private notificationService = inject(NotificationService);

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
    this.loadGlucoseChart();
    this.runAiAnalysis();

    // Reaccionar a nuevos registros
    this.refreshSub = this.glucoseService.refreshDashboard$.subscribe(() => {
      this.loadGlucoseChart();
      this.runAiAnalysis();
    });
  }

  ngOnDestroy() {
    this.refreshSub?.unsubscribe();
  }

  loadGlucoseChart() {
    this.glucoseService.getRecentReadings().subscribe({
      next: (readings) => {
        const labels = readings.map((reading) => this.formatTimeLabel(reading.fechaHora));
        const values = readings.map((reading) => reading.valorMgdl);

        this.lineChartData = {
          ...this.lineChartData,
          labels,
          datasets: [
            {
              ...this.lineChartData.datasets[0],
              data: values
            }
          ]
        };

        // Actualizar estado actual con la última lectura
        if (readings.length > 0) {
          this.currentReading = readings[readings.length - 1].valorMgdl;
          this.recentHistory = [...readings].reverse().slice(0, 3);
        }
      },
      error: () => {
        this.lineChartData = {
          ...this.lineChartData,
          labels: [],
          datasets: [
            {
              ...this.lineChartData.datasets[0],
              data: []
            }
          ]
        };
      }
    });
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
          // Validación de riesgo para notificaciones proactivas
          if (analysis.nivel_de_riesgo === 'Alto' || analysis.nivel_de_riesgo === 'Crítico') {
            this.notificationService.scheduleUrgentNotification(
              '🚨 Alerta de Riesgo IA',
              analysis.consejo_breve,
              'warning'
            );
          }
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

  formatTimeLabel(isoDate: string): string {
    const date = new Date(isoDate);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    return date.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  navigateTo(route: string) {
    this.navCtrl.navigateForward(`/${route}`);
  }

  getStatusDetails() {
    if (!this.currentReading || this.currentReading === 0) {
      return { text: 'SIN DATOS', color: '#555555' };
    }
    
    if (this.currentReading < 70) {
      return { text: 'HIPOGLUCEMIA', color: '#ff0000' };
    } else if (this.currentReading <= 140) {
      return { text: 'ESTABLE', color: '#d4ff00' };
    } else if (this.currentReading <= 180) {
      return { text: 'ALTA', color: '#ffae00' };
    } else {
      return { text: 'HIPERGLUCEMIA', color: '#ff00ff' };
    }
  }
}

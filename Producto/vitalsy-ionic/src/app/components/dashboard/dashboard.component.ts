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
  refreshOutline,
  arrowUpOutline,
  arrowDownOutline,
  arrowForwardOutline,
  trendingUpOutline,
  trendingDownOutline,
  linkOutline,
  radioOutline,
  closeOutline
} from 'ionicons/icons';
import { HeaderComponent } from '../header/header.component';
import { SensorSyncCtaComponent } from '../sensor-sync-cta/sensor-sync-cta.component';
import { IaService, IaAnalysis } from '../../services/ia.service';
import { GlucoseService } from '../../services/glucose.service';
import { NotificationService } from '../../services/notification.service';
import { LibreLinkUpService } from '../../services/librelinkup.service';
import { UserService } from '../../services/user.service';
import { SafeHtmlPipe } from '../../pipes/safe-html.pipe';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [CommonModule, IonicModule, HeaderComponent, BaseChartDirective, SafeHtmlPipe, SensorSyncCtaComponent]
})
export class DashboardComponent implements OnInit, OnDestroy {
  
  iaAnalysis: IaAnalysis | null = null;
  isIaLoading = false;
  iaError: string | null = null;
  currentReading: number = 0;
  recentHistory: any[] = [];
  
  // CTA de vinculación
  showSensorCta = false;

  // Integración LibreLinkUp
  libreConfigured = false;
  libreLastSync = '';
  isSyncing = false;
  currentTendency = 'Stable';

  // Métricas Tiempo en Rango
  tirInPercentage = 0;
  tirHighPercentage = 0;
  tirLowPercentage = 0;

  // Límites dinámicos de glucosa
  rangoGlucosaMin = 70;
  rangoGlucosaMax = 180;
  private allReadingsForTir: any[] = [];

  private refreshSub?: Subscription;
  private syncSub?: Subscription;

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
  private libreService = inject(LibreLinkUpService);
  private userService = inject(UserService);

  constructor() {
    addIcons({ 
      pulseOutline, 
      timeOutline, 
      addOutline, 
      calculatorOutline, 
      hardwareChipOutline,
      notificationsOutline,
      settingsOutline,
      refreshOutline,
      arrowUpOutline,
      arrowDownOutline,
      arrowForwardOutline,
      trendingUpOutline,
      trendingDownOutline,
      linkOutline,
      radioOutline,
      closeOutline
    });
  }

  ngOnInit() {
    this.loadUserRanges();
    this.loadGlucoseChart();
    this.runAiAnalysis();
    this.loadLibreStatus();
 
    // Reaccionar a nuevos registros manuales
    this.refreshSub = this.glucoseService.refreshDashboard$.subscribe(() => {
      this.loadUserRanges();
      this.loadGlucoseChart();
      this.runAiAnalysis();
      this.loadLibreStatus();
    });

    // Reaccionar a sync exitoso (BehaviorSubject)
    this.syncSub = this.libreService.syncSuccess$.subscribe((res) => {
      if (res) {
        this.loadUserRanges();
        this.loadGlucoseChart();
        this.runAiAnalysis();
        this.loadLibreStatus();
      }
    });
  }
 
  ngOnDestroy() {
    this.refreshSub?.unsubscribe();
    this.syncSub?.unsubscribe();
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
          const latestReading = readings[readings.length - 1];
          this.currentReading = latestReading.valorMgdl;
          this.currentTendency = latestReading.tendencia || 'Stable';
          this.recentHistory = [...readings].reverse().slice(0, 3);
          this.allReadingsForTir = readings;
          this.calculateTimeInRange(readings);
        } else {
          this.currentReading = 0;
          this.currentTendency = 'Stable';
          this.recentHistory = [];
          this.allReadingsForTir = [];
          this.calculateTimeInRange([]);
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
        this.currentReading = 0;
        this.currentTendency = 'Stable';
        this.recentHistory = [];
        this.allReadingsForTir = [];
        this.calculateTimeInRange([]);
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
    
    if (this.currentReading < this.rangoGlucosaMin) {
      return { text: 'HIPOGLUCEMIA', color: '#ff0000' };
    } else if (this.currentReading <= this.rangoGlucosaMax) {
      const range = this.rangoGlucosaMax - this.rangoGlucosaMin;
      const thresholdHigh = this.rangoGlucosaMin + (range * 0.7);
      if (this.currentReading <= thresholdHigh) {
        return { text: 'ESTABLE', color: '#d4ff00' };
      } else {
        return { text: 'ALTA', color: '#ffae00' };
      }
    } else {
      return { text: 'HIPERGLUCEMIA', color: '#ff00ff' };
    }
  }

  formatMarkdown(text: string): string {
    if (!text) return '';
    return text
      // 1. Aplicar el color neón a las negritas
      .replace(/\*\*([\s\S]+?)\*\*/g, '<strong style="color: #c6ff00; font-weight: 900;">$1</strong>')
      // 2. Si el texto empieza exactamente con un guion, cambiarlo por viñeta
      .replace(/^-\s/, '• ')
      // 3. Convertir los guiones sueltos entre palabras en saltos de línea + viñeta
      .replace(/\s-\s/g, '<br><br>• ')
      // 4. Respetar los saltos de línea originales (si es que la IA manda alguno)
      .replace(/\n/g, '<br>');
  }

  loadLibreStatus() {
    this.libreService.getStatus().subscribe({
      next: (status) => {
        this.libreConfigured = status.configurado;
        this.libreLastSync = status.ultimoSync || 'Nunca';
        // Mostrar el CTA solo si el sensor no está vinculado
        const dismissed = sessionStorage.getItem('sensorCtaDismissed');
        this.showSensorCta = !status.configurado && !dismissed;
      },
      error: () => {
        this.libreConfigured = false;
        this.libreLastSync = '';
        this.showSensorCta = false;
      }
    });
  }

  dismissSensorCta() {
    this.showSensorCta = false;
    sessionStorage.setItem('sensorCtaDismissed', '1');
  }

  loadUserRanges() {
    const minStr = localStorage.getItem('rangoGlucosaMin');
    const maxStr = localStorage.getItem('rangoGlucosaMax');

    if (minStr) this.rangoGlucosaMin = parseInt(minStr, 10);
    if (maxStr) this.rangoGlucosaMax = parseInt(maxStr, 10);

    this.userService.getUserProfile().subscribe({
      next: (profile) => {
        if (profile.rangoGlucosaMin !== undefined && profile.rangoGlucosaMin !== null) {
          this.rangoGlucosaMin = profile.rangoGlucosaMin;
          localStorage.setItem('rangoGlucosaMin', String(profile.rangoGlucosaMin));
        }
        if (profile.rangoGlucosaMax !== undefined && profile.rangoGlucosaMax !== null) {
          this.rangoGlucosaMax = profile.rangoGlucosaMax;
          localStorage.setItem('rangoGlucosaMax', String(profile.rangoGlucosaMax));
        }
        if (this.allReadingsForTir.length > 0) {
          this.calculateTimeInRange(this.allReadingsForTir);
        }
      },
      error: () => {
        console.warn('No se pudieron recuperar los rangos clínicos del servidor. Se usarán valores locales.');
      }
    });
  }

  syncSensor(event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    if (this.isSyncing) return;

    this.isSyncing = true;
    this.libreService.forceSync().subscribe({
      next: () => {
        this.isSyncing = false;
      },
      error: (err) => {
        console.error(err);
        this.isSyncing = false;
      }
    });
  }

  formatLastSync(isoDate: string): string {
    if (!isoDate || isoDate === 'Nunca') return 'Nunca';
    const date = new Date(isoDate);
    if (Number.isNaN(date.getTime())) return isoDate;
    
    const diffMs = Date.now() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Hace un momento';
    if (diffMins < 60) return `Hace ${diffMins} min`;
    
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `Hace ${diffHours} h`;
    
    return date.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' }) + ' ' + date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  getTrendArrowSymbol(): { icon: string, symbol: string, color: string } {
    switch (this.currentTendency) {
      case 'RisingFast':
        return { icon: 'arrow-up-outline', symbol: '↑↑', color: '#ff00ff' };
      case 'Rising':
        return { icon: 'trending-up-outline', symbol: '↑', color: '#ccff00' };
      case 'Stable':
        return { icon: 'arrow-forward-outline', symbol: '→', color: '#ccff00' };
      case 'Falling':
        return { icon: 'trending-down-outline', symbol: '↓', color: '#ffae00' };
      case 'FallingFast':
        return { icon: 'arrow-down-outline', symbol: '↓↓', color: '#ff0000' };
      default:
        return { icon: 'arrow-forward-outline', symbol: '→', color: '#a1a1aa' };
    }
  }

  calculateTimeInRange(readings: any[]) {
    if (!readings || readings.length === 0) {
      this.tirInPercentage = 0;
      this.tirHighPercentage = 0;
      this.tirLowPercentage = 0;
      return;
    }

    const total = readings.length;
    let inRange = 0;
    let low = 0;
    let high = 0;

    readings.forEach((reading) => {
      const val = reading.valorMgdl;
      if (val < this.rangoGlucosaMin) {
        low++;
      } else if (val <= this.rangoGlucosaMax) {
        inRange++;
      } else {
        high++;
      }
    });

    this.tirInPercentage = Math.round((inRange / total) * 100);
    this.tirLowPercentage = Math.round((low / total) * 100);
    this.tirHighPercentage = Math.round((high / total) * 100);
  }
}

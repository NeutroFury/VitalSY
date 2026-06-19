import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, ModalController } from '@ionic/angular';
import { IaService } from '../../services/ia.service';
import { RecordatorioService } from '../../services/recordatorio.service';
import { addIcons } from 'ionicons';
import { notificationsOutline, warningOutline, timeOutline, closeOutline, alertCircleOutline, pulseOutline, medkitOutline, waterOutline } from 'ionicons/icons';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-notification-history',
  templateUrl: './notification-history.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule]
})
export class NotificationHistoryComponent implements OnInit {
  private iaService = inject(IaService);
  private recordatorioService = inject(RecordatorioService);
  private modalCtrl = inject(ModalController);

  alertas: any[] = [];
  recordatorios: any[] = [];
  isLoading = true;

  constructor() {
    addIcons({ notificationsOutline, warningOutline, timeOutline, closeOutline, alertCircleOutline, pulseOutline, medkitOutline, waterOutline });
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    
    // Fetch Recordatorios
    this.recordatorioService.getRecordatorios().subscribe({
      next: (recs) => {
        // Solo mostrar recordatorios activos
        this.recordatorios = recs.filter((r: any) => r.activo);
      },
      error: (e) => console.error(e)
    });

    // Fetch Alertas Predictivas
    this.iaService.getPredictiveAnalysis(7).pipe(
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (res) => {
        if (res && res.predictive_alerts) {
          this.alertas = res.predictive_alerts;
        }
      },
      error: (e) => console.error(e)
    });
  }

  close() {
    this.modalCtrl.dismiss();
  }

  getProbabilityPercentage(prob: number): number {
    return Math.round(prob * 100);
  }

  getRecordatorioIcon(tipo: string): string {
    switch(tipo?.toUpperCase()) {
      case 'MEDICACION': return 'medkit-outline';
      case 'MEDICION': return 'pulse-outline';
      case 'COMIDA': return 'water-outline';
      default: return 'notifications-outline';
    }
  }
}

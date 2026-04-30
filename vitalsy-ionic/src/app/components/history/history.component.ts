import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { 
  waterOutline, 
  nutritionOutline, 
  arrowForwardOutline, 
  downloadOutline,
  timeOutline,
  calendarOutline,
  chevronBackOutline,
  chevronDownCircleOutline,
  fastFoodOutline,
  chatbubbleOutline,
  hardwareChipOutline
} from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';
import { GlucoseService, GlucoseReadingDto } from '../../services/glucose.service';

interface GroupedReadings {
  dateLabel: string;
  readings: GlucoseReadingDto[];
}

@Component({
  selector: 'app-history',
  templateUrl: './history.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule, HeaderComponent]
})
export class HistoryComponent implements OnInit {
  
  private glucoseService = inject(GlucoseService);
  groupedHistory: GroupedReadings[] = [];
  isLoading = false;

  constructor() {
    addIcons({ 
      waterOutline, 
      nutritionOutline, 
      arrowForwardOutline, 
      downloadOutline,
      timeOutline,
      calendarOutline,
      chevronBackOutline,
      chevronDownCircleOutline,
      fastFoodOutline,
      chatbubbleOutline,
      hardwareChipOutline
    });
  }

  ngOnInit() {
    this.loadHistory();
  }

  loadHistory() {
    this.isLoading = true;
    this.glucoseService.getAllReadings().subscribe({
      next: (readings) => {
        this.groupedHistory = this.groupReadingsByDate(readings);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('HISTORY: Error loading data', err);
        this.isLoading = false;
      }
    });
  }

  private groupReadingsByDate(readings: GlucoseReadingDto[]): GroupedReadings[] {
    const groups: { [key: string]: GlucoseReadingDto[] } = {};
    
    // Ordenamos por fecha descendente
    const sorted = [...readings].sort((a, b) => 
      new Date(b.fechaHora).getTime() - new Date(a.fechaHora).getTime()
    );

    sorted.forEach(reading => {
      const date = new Date(reading.fechaHora);
      const label = this.getDateLabel(date);
      if (!groups[label]) {
        groups[label] = [];
      }
      groups[label].push(reading);
    });

    return Object.keys(groups).map(label => ({
      dateLabel: label,
      readings: groups[label]
    }));
  }

  private getDateLabel(date: Date): string {
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);

    if (this.isSameDay(date, today)) return 'Hoy';
    if (this.isSameDay(date, yesterday)) return 'Ayer';

    return date.toLocaleDateString('es-ES', { 
      day: 'numeric', 
      month: 'long'
    });
  }

  private isSameDay(d1: Date, d2: Date): boolean {
    return d1.getDate() === d2.getDate() &&
           d1.getMonth() === d2.getMonth() &&
           d1.getFullYear() === d2.getFullYear();
  }

  getReadingStyle(valor: number) {
    if (valor < 70) return { color: '#ff0000', label: 'BAJA' };
    if (valor <= 140) return { color: '#d4ff00', label: 'ESTABLE' };
    if (valor <= 180) return { color: '#ffae00', label: 'ALTA' };
    return { color: '#ff00ff', label: 'CRÍTICA' };
  }

  formatTime(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleTimeString('es-ES', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: true 
    });
  }

  formatDateShort(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-ES', { 
      day: 'numeric', 
      month: 'short',
      year: 'numeric'
    });
  }

  handleRefresh(event: any) {
    this.glucoseService.getAllReadings().subscribe({
      next: (readings) => {
        this.groupedHistory = this.groupReadingsByDate(readings);
        event.target.complete();
      },
      error: (err) => {
        console.error('HISTORY: Error refreshing data', err);
        event.target.complete();
      }
    });
  }

  exportData() {
    if (this.groupedHistory.length === 0) return;

    const allReadings = this.groupedHistory.reduce((acc: any[], group) => {
      return acc.concat(group.readings);
    }, []);

    const headers = ['Fecha y Hora', 'Valor (mg/dL)', 'Tendencia', 'Tipo de Registro'];
    
    const csvContent = [
      headers.join(','),
      ...allReadings.map((r: any) => [
        new Date(r.fechaHora).toLocaleString(),
        r.valorMgdl,
        r.tendencia || 'Estable',
        r.tipoRegistro
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `VitalSY_Reporte_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}

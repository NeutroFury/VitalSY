import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Capacitor } from '@capacitor/core';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';
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
  expandedDays: string[] = [];
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
        this.expandedDays = this.groupedHistory.length > 0 
          ? [this.groupedHistory[0].dateLabel] 
          : [];
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
        this.expandedDays = this.groupedHistory.length > 0 
          ? [this.groupedHistory[0].dateLabel] 
          : [];
        event.target.complete();
      },
      error: (err) => {
        console.error('HISTORY: Error refreshing data', err);
        event.target.complete();
      }
    });
  }

  exportData() {
    this.isLoading = true;
    this.glucoseService.exportarPdf().subscribe({
      next: async (blob) => {
        try {
          if (Capacitor.isNativePlatform()) {
            const reader = new FileReader();
            reader.readAsDataURL(blob);
            reader.onloadend = async () => {
              const base64data = reader.result as string;
              const base64 = base64data.split(',')[1];
              const fileName = `VitalSY_Reporte_${new Date().toISOString().split('T')[0]}.pdf`;
              
              const savedFile = await Filesystem.writeFile({
                path: fileName,
                data: base64,
                directory: Directory.Cache
              });

              await Share.share({
                title: 'Reporte Clínico VitalSY',
                text: 'Adjunto reporte clínico de glucosa de VitalSY',
                url: savedFile.uri,
                dialogTitle: 'Compartir o Guardar Reporte PDF'
              });
              this.isLoading = false;
            };
          } else {
            const link = document.createElement('a');
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', `VitalSY_Reporte_${new Date().toISOString().split('T')[0]}.pdf`);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            this.isLoading = false;
          }
        } catch (e) {
          console.error('Error guardando PDF nativo', e);
          this.isLoading = false;
        }
      },
      error: (err) => {
        console.error('HISTORY: Error exporting PDF', err);
        this.isLoading = false;
      }
    });
  }
}

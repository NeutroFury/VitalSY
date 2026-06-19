import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, ToastController } from '@ionic/angular';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { addIcons } from 'ionicons';
import { alarmOutline, addOutline, trashOutline, waterOutline, pulseOutline, restaurantOutline, closeOutline, timeOutline, calendarOutline } from 'ionicons/icons';
import { RecordatorioService, RecordatorioResponse, RecordatorioRequest } from '../../services/recordatorio.service';
import { LocalReminderService } from '../../services/local-reminder.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-recordatorios',
  templateUrl: './recordatorios.component.html',
  styleUrls: ['./recordatorios.component.scss'],
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule, ReactiveFormsModule]
})
export class RecordatoriosComponent implements OnInit {
  recordatorios: RecordatorioResponse[] = [];
  isLoading = false;
  isSaving = false;
  
  isModalOpen = false;
  recordatorioForm: FormGroup;
  editingId: number | null = null;

  diasSemana = [
    { id: 1, label: 'L' },
    { id: 2, label: 'M' },
    { id: 3, label: 'X' },
    { id: 4, label: 'J' },
    { id: 5, label: 'V' },
    { id: 6, label: 'S' },
    { id: 7, label: 'D' }
  ];

  tipos = [
    { id: 'MEDICION_GLUCOSA', label: 'Glucosa', icon: 'pulse-outline' },
    { id: 'APLICACION_INSULINA', label: 'Insulina', icon: 'water-outline' },
    { id: 'COMIDA', label: 'Comida', icon: 'restaurant-outline' }
  ];

  private recordatorioService = inject(RecordatorioService);
  private localReminderService = inject(LocalReminderService);
  private toastCtrl = inject(ToastController);
  private fb = inject(FormBuilder);

  constructor() {
    addIcons({ alarmOutline, addOutline, trashOutline, waterOutline, pulseOutline, restaurantOutline, closeOutline, timeOutline, calendarOutline });
    this.recordatorioForm = this.fb.group({
      tipo: ['MEDICION_GLUCOSA', Validators.required],
      hora: ['08:00', Validators.required],
      diasRepeticion: [[1, 2, 3, 4, 5], Validators.required],
      activo: [true]
    });
  }

  ngOnInit() {
    this.loadRecordatorios();
  }

  loadRecordatorios() {
    this.isLoading = true;
    this.recordatorioService.getRecordatorios()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (data) => {
          this.recordatorios = data;
          this.syncLocal();
        },
        error: () => this.showToast('Error cargando recordatorios', 'danger')
      });
  }

  openModal(rec?: RecordatorioResponse) {
    if (rec) {
      this.editingId = rec.id;
      this.recordatorioForm.patchValue({
        tipo: rec.tipo,
        hora: rec.hora.substring(0, 5),
        diasRepeticion: rec.diasRepeticion.split(',').map(d => parseInt(d.trim(), 10)),
        activo: rec.activo
      });
    } else {
      this.editingId = null;
      this.recordatorioForm.reset({
        tipo: 'MEDICION_GLUCOSA',
        hora: '08:00',
        diasRepeticion: [1, 2, 3, 4, 5],
        activo: true
      });
    }
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  toggleDia(diaId: number) {
    const current = this.recordatorioForm.get('diasRepeticion')?.value as number[] || [];
    if (current.includes(diaId)) {
      this.recordatorioForm.patchValue({ diasRepeticion: current.filter(d => d !== diaId) });
    } else {
      this.recordatorioForm.patchValue({ diasRepeticion: [...current, diaId].sort() });
    }
  }

  saveRecordatorio() {
    if (this.recordatorioForm.invalid) {
      this.showToast('Completa todos los campos obligatorios', 'warning');
      return;
    }

    const val = this.recordatorioForm.value;
    
    let finalHora = val.hora;
    if (finalHora.includes('T')) {
      const date = new Date(finalHora);
      const h = String(date.getHours()).padStart(2, '0');
      const m = String(date.getMinutes()).padStart(2, '0');
      finalHora = `${h}:${m}:00`;
    } else if (finalHora.length === 5) {
      finalHora = finalHora + ':00';
    }

    const req: RecordatorioRequest = {
      tipo: val.tipo,
      hora: finalHora,
      diasRepeticion: val.diasRepeticion.join(','),
      activo: val.activo
    };

    this.isSaving = true;

    if (this.editingId) {
      this.recordatorioService.updateRecordatorio(this.editingId, req)
        .pipe(finalize(() => this.isSaving = false))
        .subscribe({
          next: () => {
            this.showToast('Recordatorio actualizado', 'success');
            this.closeModal();
            this.loadRecordatorios();
          },
          error: () => this.showToast('Error actualizando', 'danger')
        });
    } else {
      this.recordatorioService.createRecordatorio(req)
        .pipe(finalize(() => this.isSaving = false))
        .subscribe({
          next: () => {
            this.showToast('Recordatorio creado', 'success');
            this.closeModal();
            this.loadRecordatorios();
          },
          error: () => this.showToast('Error creando', 'danger')
        });
    }
  }

  deleteRecordatorio(id: number) {
    this.recordatorioService.deleteRecordatorio(id).subscribe({
      next: () => {
        this.showToast('Recordatorio eliminado', 'success');
        this.isModalOpen = false;
        this.loadRecordatorios();
      },
      error: () => this.showToast('Error eliminando', 'danger')
    });
  }

  toggleActivo(rec: RecordatorioResponse) {
    let finalHora = rec.hora;
    if (finalHora && finalHora.length === 5) {
      finalHora = finalHora + ':00';
    } else if (finalHora.includes('T')) {
      const date = new Date(finalHora);
      const h = String(date.getHours()).padStart(2, '0');
      const m = String(date.getMinutes()).padStart(2, '0');
      finalHora = `${h}:${m}:00`;
    }

    const req: RecordatorioRequest = {
      tipo: rec.tipo,
      hora: finalHora,
      diasRepeticion: rec.diasRepeticion,
      activo: !rec.activo
    };
    
    this.recordatorioService.updateRecordatorio(rec.id, req).subscribe({
      next: () => {
        rec.activo = !rec.activo;
        this.syncLocal();
      },
      error: () => {
        this.showToast('Error al cambiar estado', 'danger');
      }
    });
  }

  private syncLocal() {
    this.localReminderService.syncLocalNotifications(this.recordatorios);
  }

  getIcon(tipo: string): string {
    return this.tipos.find(t => t.id === tipo)?.icon || 'alarm-outline';
  }

  getDiasStr(diasStr: string): string {
    const arr = diasStr.split(',').map(d => parseInt(d.trim(), 10));
    return arr.map(d => this.diasSemana.find(ds => ds.id === d)?.label).join(' ');
  }

  private async showToast(message: string, color: string) {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2000,
      color,
      position: 'bottom',
      cssClass: 'vitalsy-toast'
    });
    await toast.present();
  }
}

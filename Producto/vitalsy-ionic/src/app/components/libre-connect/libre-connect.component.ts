import { Component, inject, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, ToastController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { linkOutline, informationCircleOutline, radioOutline } from 'ionicons/icons';
import { LibreLinkUpService } from '../../services/librelinkup.service';

@Component({
  selector: 'app-libre-connect',
  templateUrl: './libre-connect.component.html',
  styleUrls: ['./libre-connect.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule]
})
export class LibreConnectComponent {
  @Output() connected = new EventEmitter<void>();

  email = '';
  password = '';
  isLoading = false;

  private libreService = inject(LibreLinkUpService);
  private toastController = inject(ToastController);

  constructor() {
    addIcons({ linkOutline, informationCircleOutline, radioOutline });
  }

  async connect() {
    if (!this.email || !this.password) return;

    this.isLoading = true;

    this.libreService.setup({ email: this.email, password: this.password }).subscribe({
      next: async (res) => {
        this.isLoading = false;
        await this.showToast(res?.mensaje || 'Sensor vinculado exitosamente', 'success');
        this.email = '';
        this.password = '';
        this.connected.emit();
      },
      error: async (err) => {
        this.isLoading = false;
        const msg = err?.error?.mensaje || 'Credenciales incorrectas. Verifica tu cuenta de LibreView.';
        await this.showToast(msg, 'danger');
      }
    });
  }

  async showToast(message: string, color: 'success' | 'danger') {
    const toast = await this.toastController.create({
      message,
      duration: 3500,
      color,
      position: 'bottom'
    });
    await toast.present();
  }
}

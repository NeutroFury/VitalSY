import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, ToastController, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { eye, eyeOff, lockClosedOutline } from 'ionicons/icons';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule]
})
export class ResetPasswordComponent implements OnInit {
  token: string | null = null;
  newPassword = '';
  confirmPassword = '';
  showPassword = false;
  isSubmitting = false;
  isNewPasswordFocused = false;
  isConfirmPasswordFocused = false;

  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private toastController = inject(ToastController);
  private navCtrl = inject(NavController);

  constructor() {
    addIcons({ eye, eyeOff, lockClosedOutline });
  }

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  async presentToast(message: string, color: 'success' | 'danger') {
    const toast = await this.toastController.create({
      message,
      duration: 3000,
      color,
      position: 'top',
    });
    await toast.present();
  }

  async onSubmit() {
    if (!this.token) {
      this.presentToast('Enlace inválido o sin token. Vuelve a solicitar el cambio.', 'danger');
      return;
    }

    if (this.newPassword.length < 6) {
      this.presentToast('La contraseña debe tener al menos 6 caracteres', 'danger');
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.presentToast('Las contraseñas no coinciden', 'danger');
      return;
    }

    this.isSubmitting = true;

    this.authService.resetPassword({ token: this.token, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.presentToast('Contraseña actualizada exitosamente', 'success');
        this.navCtrl.navigateRoot('/login', { animated: true, animationDirection: 'back' });
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err.error?.message || 'Error al restablecer la contraseña. El enlace puede haber expirado.';
        this.presentToast(msg, 'danger');
      }
    });
  }
}

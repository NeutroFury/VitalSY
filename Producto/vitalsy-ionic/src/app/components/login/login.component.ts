import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController, AlertController, ToastController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { alertCircleOutline, pulse } from 'ionicons/icons';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule]
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage: string | null = null;
  
  private authService = inject(AuthService);
  private navCtrl = inject(NavController);
  private router = inject(Router);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);

  constructor() {
    addIcons({ pulse, alertCircleOutline });
  }


  onSubmit() {
    this.clearError();
    if (this.email && this.password) {
      const credentials = {
        email: this.email,
        password: this.password
      };

      this.authService.login(credentials).subscribe({
        next: (res) => {
          const rol = this.authService.getRol();
          const destino = rol === 'ADMIN' ? '/admin-panel' : '/dashboard';
          this.navCtrl.navigateRoot(destino, {
            animated: true,
            animationDirection: 'forward'
          });
        },
        error: (err) => {
          console.error('Login failed', err);
          this.errorMessage = 'Correo o contraseña incorrectos.';
          this.email = '';
          this.password = '';
        }
      });
    }
  }

  clearError() {
    this.errorMessage = null;
  }

  goToSignup() {
    this.router.navigate(['/register']);
  }

  async promptForgotPassword() {
    const alert = await this.alertCtrl.create({
      header: 'Recuperar contraseña',
      message: 'Ingresa tu correo electrónico y te enviaremos las instrucciones para restablecer tu contraseña.',
      inputs: [
        {
          name: 'email',
          type: 'email',
          placeholder: 'tu@correo.com',
          attributes: {
            required: true
          }
        }
      ],
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel',
          cssClass: 'text-zinc-400'
        },
        {
          text: 'Enviar',
          handler: (data) => {
            if (data.email) {
              this.sendForgotPasswordRequest(data.email);
            }
            return true;
          }
        }
      ]
    });

    await alert.present();
  }

  private sendForgotPasswordRequest(email: string) {
    this.authService.forgotPassword(email).subscribe({
      next: async (res) => {
        const toast = await this.toastCtrl.create({
          message: res.message || 'Si el correo está registrado, recibirás un enlace.',
          duration: 4000,
          color: 'success',
          position: 'top'
        });
        toast.present();
      },
      error: async (err) => {
        const toast = await this.toastCtrl.create({
          message: 'Error al solicitar recuperación. Inténtalo de nuevo más tarde.',
          duration: 3000,
          color: 'danger',
          position: 'top'
        });
        toast.present();
      }
    });
  }
}

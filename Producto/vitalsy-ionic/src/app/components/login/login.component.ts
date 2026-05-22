import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
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
          console.log('Login successful', res);
          this.navCtrl.navigateRoot('/dashboard', { 
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
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { pulse } from 'ionicons/icons';
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
  
  private authService = inject(AuthService);
  private navCtrl = inject(NavController);
  private router = inject(Router);

  constructor() {
    addIcons({ pulse });
  }

  onSubmit() {
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
          // Here you could add a toast notification for the user
        }
      });
    }
  }

  goToSignup() {
    this.router.navigate(['/register']);
  }
}

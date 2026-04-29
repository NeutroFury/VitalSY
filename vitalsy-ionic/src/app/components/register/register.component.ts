import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { personAddOutline, chevronBackOutline } from 'ionicons/icons';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule]
})
export class RegisterComponent {
  userData = {
    email: '',
    password: '',
    nombre: '',
    fechaNacimiento: '1990-01-01',
    genero: 'Masculino',
    pesoActual: 70.0
  };
  
  private authService = inject(AuthService);
  private navCtrl = inject(NavController);
  private router = inject(Router);

  constructor() {
    addIcons({ personAddOutline, chevronBackOutline });
  }

  onRegister() {
    this.authService.register(this.userData).subscribe({
      next: (res) => {
        console.log('Registration successful', res);
        // Usually after register we go to login or auto-login
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Registration failed', err);
      }
    });
  }

  goBack() {
    this.navCtrl.back();
  }
}

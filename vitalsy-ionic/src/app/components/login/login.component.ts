import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { pulse } from 'ionicons/icons';

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

  constructor(private navCtrl: NavController) {
    addIcons({ pulse });
  }

  onSubmit() {
    if (this.email && this.password) {
      console.log('Login attempt with:', { email: this.email, password: this.password });
      // Aquí iría la lógica real de autenticación
      
      // Navegación suave estilo nativo al Dashboard
      this.navCtrl.navigateRoot('/dashboard', { 
        animated: true, 
        animationDirection: 'forward' 
      });
    }
  }

  goToSignup() {
    console.log('Navegar a registro');
    // Implementar navegación a /signup
  }
}

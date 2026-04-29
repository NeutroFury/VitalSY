import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { personOutline, settingsOutline, fitnessOutline, logOutOutline, linkOutline } from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent]
})
export class ProfileComponent {
  // Información Personal
  userName = 'Gabriel';
  userAge = '28';
  weightProgress = '75';
  dietType = 'Keto';

  // Abbott
  isAbbottConnected = true;

  // Metas de Glucosa
  glucoseMin = '70';
  glucoseMax = '180';

  constructor(private navCtrl: NavController) {
    addIcons({ personOutline, settingsOutline, fitnessOutline, logOutOutline, linkOutline });
  }

  toggleAbbott() {
    this.isAbbottConnected = !this.isAbbottConnected;
  }

  logout() {
    console.log('Cerrando sesión...');
    // Redirección suave al Login como en una app nativa
    this.navCtrl.navigateRoot('/login', { animated: true, animationDirection: 'back' });
  }
}

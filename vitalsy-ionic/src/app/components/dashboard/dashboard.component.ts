import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { 
  pulseOutline, 
  timeOutline, 
  addOutline, 
  calculatorOutline, 
  hardwareChipOutline, 
  notificationsOutline, 
  settingsOutline 
} from 'ionicons/icons';

import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule, HeaderComponent]
})
export class DashboardComponent {
  
  constructor(private navCtrl: NavController) {
    addIcons({ 
      pulseOutline, 
      timeOutline, 
      addOutline, 
      calculatorOutline, 
      hardwareChipOutline,
      notificationsOutline,
      settingsOutline
    });
  }

  navigateTo(route: string) {
    this.navCtrl.navigateForward(`/${route}`);
  }
}

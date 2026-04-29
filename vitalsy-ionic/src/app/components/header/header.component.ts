import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { pulse, chevronBackOutline, notificationsOutline, settingsOutline } from 'ionicons/icons';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  standalone: true,
  imports: [CommonModule, IonicModule]
})
export class HeaderComponent {
  @Input() showBack: boolean = false;
  @Input() showGreeting: boolean = true;
  @Input() showActions: boolean = false;
  @Input() title: string = '';

  constructor(private navCtrl: NavController) {
    addIcons({ pulse, chevronBackOutline, notificationsOutline, settingsOutline });
  }

  goBack() {
    this.navCtrl.back();
  }

  navigateTo(route: string) {
    this.navCtrl.navigateForward(`/${route}`);
  }
}

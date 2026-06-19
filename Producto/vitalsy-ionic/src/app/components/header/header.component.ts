import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, NavController, ModalController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { pulse, chevronBackOutline, notificationsOutline, settingsOutline } from 'ionicons/icons';
import { AuthService } from '../../services/auth.service';
import { NotificationHistoryComponent } from '../notification-history/notification-history.component';

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

  private authService = inject(AuthService);
  private modalCtrl = inject(ModalController);
  username: string = '';

  constructor(private navCtrl: NavController) {
    addIcons({ pulse, chevronBackOutline, notificationsOutline, settingsOutline });
    this.username = this.authService.getUsername();
  }

  goBack() {
    this.navCtrl.back();
  }

  navigateTo(route: string) {
    this.navCtrl.navigateForward(`/${route}`);
  }

  async openAlertSettings() {
    const modal = await this.modalCtrl.create({
      component: NotificationHistoryComponent,
      initialBreakpoint: 0.85,
      breakpoints: [0, 0.85, 1],
      handle: true
    });
    await modal.present();
  }
}

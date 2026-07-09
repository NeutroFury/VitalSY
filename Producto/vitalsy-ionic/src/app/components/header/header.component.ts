import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, NavController, ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { addIcons } from 'ionicons';
import { 
  pulse, 
  pulseOutline, 
  timeOutline, 
  addOutline, 
  calculatorOutline, 
  personOutline, 
  chatbubbleOutline, 
  chevronBackOutline, 
  notificationsOutline, 
  settingsOutline 
} from 'ionicons/icons';
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
  @Input() icon: string = '';

  private authService = inject(AuthService);
  private modalCtrl = inject(ModalController);
  private router = inject(Router);
  username: string = '';

  constructor(private navCtrl: NavController) {
    addIcons({ 
      pulse, 
      pulseOutline, 
      timeOutline, 
      addOutline, 
      calculatorOutline, 
      personOutline, 
      chatbubbleOutline, 
      chevronBackOutline, 
      notificationsOutline, 
      settingsOutline 
    });
    this.username = this.authService.getUsername();
  }

  getHeaderIcon(): string {
    const url = this.router.url;
    if (url.includes('/dashboard')) return 'pulse-outline';
    if (url.includes('/history')) return 'time-outline';
    if (url.includes('/registro')) return 'add-outline';
    if (url.includes('/calculator')) return 'calculator-outline';
    if (url.includes('/chat')) return 'chatbubble-outline';
    if (url.includes('/profile')) return 'person-outline';
    return 'pulse';
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

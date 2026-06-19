import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { RecordatorioService } from './services/recordatorio.service';
import { LocalReminderService } from './services/local-reminder.service';
import { NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { pulseOutline, timeOutline, addOutline, calculatorOutline, personOutline } from 'ionicons/icons';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
  standalone: false,
})
export class AppComponent implements OnInit {
  private authService = inject(AuthService);
  private recordatorioService = inject(RecordatorioService);
  private localReminderService = inject(LocalReminderService);
  private navCtrl = inject(NavController);

  public isLoggedIn$ = this.authService.isLoggedIn$;

  constructor() {
    addIcons({ pulseOutline, timeOutline, addOutline, calculatorOutline, personOutline });
  }

  ngOnInit() {
    this.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.recordatorioService.getRecordatorios().subscribe({
          next: (recordatorios) => {
            this.localReminderService.syncLocalNotifications(recordatorios);
          },
          error: (err) => console.error('Error sincronizando recordatorios al iniciar sesión', err)
        });
      }
    });
  }

  navigateTo(path: string) {
    this.navCtrl.navigateRoot(`/${path}`, { animated: false });
  }
}

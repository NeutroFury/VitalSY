import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { RecordatorioService } from './services/recordatorio.service';
import { LocalReminderService } from './services/local-reminder.service';

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

  constructor() {}

  ngOnInit() {
    this.authService.isLoggedIn$.subscribe(isLoggedIn => {
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
}

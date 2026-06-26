import { Component, OnInit, inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './services/auth.service';
import { RecordatorioService } from './services/recordatorio.service';
import { LocalReminderService } from './services/local-reminder.service';
import { NavController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { pulseOutline, timeOutline, addOutline, calculatorOutline, personOutline, chatbubbleOutline } from 'ionicons/icons';

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
  private router = inject(Router);

  public isLoggedIn$ = this.authService.isLoggedIn$;
  public showTabs = false;
  public currentRoute = '';

  constructor() {
    addIcons({ pulseOutline, timeOutline, addOutline, calculatorOutline, personOutline, chatbubbleOutline });
  }

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      const url = event.urlAfterRedirects || event.url;
      this.showTabs = !url.includes('/login') && !url.includes('/register') && !url.includes('/reset-password');
      
      // Determine the active route
      if (url.includes('/dashboard')) this.currentRoute = 'dashboard';
      else if (url.includes('/history')) this.currentRoute = 'history';
      else if (url.includes('/registro')) this.currentRoute = 'registro';
      else if (url.includes('/calculator')) this.currentRoute = 'calculator';
      else if (url.includes('/chat')) this.currentRoute = 'chat';
      else if (url.includes('/profile')) this.currentRoute = 'profile';
      else this.currentRoute = '';
    });

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

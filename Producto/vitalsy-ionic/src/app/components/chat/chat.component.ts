import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, IonContent } from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  hardwareChipOutline,
  send,
  pulseOutline,
  nutritionOutline,
  trendingUpOutline,
  mic
} from 'ionicons/icons';
import { SpeechRecognition } from '@capacitor-community/speech-recognition';
import { TextToSpeech } from '@capacitor-community/text-to-speech';
import { HeaderComponent } from '../header/header.component';
import { IaService } from '../../services/ia.service';
import { SafeHtmlPipe } from '../../pipes/safe-html.pipe';

interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface Suggestion {
  id: number;
  label: string;
  prompt: string;
  icon: string;
}

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, HeaderComponent, SafeHtmlPipe]
})
export class ChatComponent {

  @ViewChild(IonContent, { static: false }) content!: IonContent;
  @ViewChild('messageInput') private messageInput!: ElementRef;

  messages: ChatMessage[] = [];
  userMessage = '';
  isLoading = false;
  isListening = false;
  private messageIdCounter = 0;

  private iaService = inject(IaService);

  suggestions: Suggestion[] = [
    {
      id: 1,
      label: 'Estado Actual',
      prompt: '¿Cómo están mis niveles de glucosa hoy?',
      icon: 'pulse-outline'
    },
    {
      id: 2,
      label: 'Consejos',
      prompt: 'Dame consejos personalizados para mejorar mi control glucémico',
      icon: 'nutrition-outline'
    },
    {
      id: 3,
      label: 'Tendencia',
      prompt: 'Analiza mi tendencia de glucosa de esta semana y dime si debo preocuparme',
      icon: 'trending-up-outline'
    }
  ];

  constructor() {
    addIcons({
      hardwareChipOutline,
      send,
      pulseOutline,
      nutritionOutline,
      trendingUpOutline,
      mic
    });
  }

  sendSuggestion(prompt: string): void {
    this.userMessage = prompt;
    this.sendMessage();
  }

  sendMessage(): void {
    const content = this.userMessage.trim();
    if (!content || this.isLoading) return;

    // Add user message
    this.messages.push({
      id: ++this.messageIdCounter,
      role: 'user',
      content,
      timestamp: new Date()
    });

    this.userMessage = '';
    this.isLoading = true;
    this.scrollToBottom();

    // Call the real IA chat endpoint
    this.iaService.enviarMensajeChat(content).subscribe({
      next: (res) => {
        this.messages.push({
          id: ++this.messageIdCounter,
          role: 'assistant',
          content: res.respuesta,
          timestamp: new Date()
        });
        this.isLoading = false;
        this.scrollToBottom();
        // this.speakResponse(res.respuesta); // Lectura automática desactivada a petición del usuario
      },
      error: () => {
        this.messages.push({
          id: ++this.messageIdCounter,
          role: 'assistant',
          content: 'Lo siento, no pude conectarme con el servicio de análisis en este momento. Intenta de nuevo en unos segundos.',
          timestamp: new Date()
        });
        this.isLoading = false;
        this.scrollToBottom();
      }
    });
  }

  onEnterPress(event: Event): void {
    const keyEvent = event as KeyboardEvent;
    if (!keyEvent.shiftKey) {
      keyEvent.preventDefault();
      this.sendMessage();
    }
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatMarkdown(text: string): string {
    if (!text) return '';
    return text
      // 1. Aplicar el color neón a las negritas
      .replace(/\*\*([\s\S]+?)\*\*/g, '<strong style="color: #c6ff00; font-weight: 900;">$1</strong>')
      // 2. Si el texto empieza exactamente con un guion, cambiarlo por viñeta
      .replace(/^-\s/, '• ')
      // 3. Convertir los guiones sueltos entre palabras en saltos de línea + viñeta
      .replace(/\s-\s/g, '<br><br>• ')
      // 4. Respetar los saltos de línea originales (si es que la IA manda alguno)
      .replace(/\n/g, '<br>');
  }

  async startListening() {
    try {
      const hasPermission = await SpeechRecognition.checkPermissions();
      if (hasPermission.speechRecognition !== 'granted') {
        await SpeechRecognition.requestPermissions();
      }

      this.isListening = true;
      SpeechRecognition.start({
        language: 'es-ES',
        maxResults: 1,
        prompt: 'Habla ahora...',
        partialResults: false
      }).then(result => {
        if (result.matches && result.matches.length > 0) {
          this.userMessage = (this.userMessage + ' ' + result.matches[0]).trim();
        }
        this.isListening = false;
      }).catch(err => {
        console.error('STT Error:', err);
        this.isListening = false;
      });
    } catch (e) {
      console.error('Error starting speech recognition', e);
      this.isListening = false;
    }
  }

  async speakResponse(text: string) {
    // Método vaciado para eliminar por completo la lectura en voz alta
    return;
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.content) {
        this.content.scrollToBottom(300);
      }
    }, 100);
  }
}
